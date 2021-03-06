package com.skelril.skree.service.internal.market;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.skelril.nitro.Clause;
import com.skelril.nitro.probability.Probability;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.db.SQLHandle;
import com.skelril.skree.db.schema.tables.records.ItemDataRecord;
import com.skelril.skree.service.MarketService;
import com.skelril.skree.service.internal.market.deducer.DeducerOfSimpleType;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.skelril.skree.db.schema.tables.ItemAliases.ITEM_ALIASES;
import static com.skelril.skree.db.schema.tables.ItemData.ITEM_DATA;

public class MarketServiceImpl implements MarketService {

  private final Path marketValuesPath;

  public MarketServiceImpl(Path marketValuesPath) {
    this.marketValuesPath = marketValuesPath;
  }

  private void validateAlias(String alias) {
    if (!alias.matches(VALID_ALIAS_REGEX)) {
      throw new IllegalArgumentException("Aliases must match the pattern " + VALID_ALIAS_REGEX);
    }
  }

  @Override
  public Optional<ItemStack> getItem(String alias) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Record2<String, String> result = create.select(ITEM_DATA.MC_ID, ITEM_DATA.VARIANT)
          .from(ITEM_DATA)
          .where(ITEM_DATA.ID.equal(
              create.select(ITEM_ALIASES.ITEM_ID)
                  .from(ITEM_ALIASES)
                  .where(ITEM_ALIASES.ALIAS.equal(alias.toLowerCase()))
              )
          ).fetchOne();
      return result == null ? Optional.empty() : Optional.ofNullable(
          getItemStack(
              new Clause<>(
                  result.getValue(ITEM_DATA.MC_ID),
                  result.getValue(ITEM_DATA.VARIANT)
              )
          )
      );
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public BigDecimal getSellFactor(BigDecimal buyPrice) {
    return new BigDecimal(0.7);
  }

  private BigDecimal getNewValue(BigDecimal baseValue) {
    double random = 1D / (5 + Probability.getRandom(5));

    BigDecimal multiplier = new BigDecimal(random);
    BigDecimal change = baseValue.multiply(multiplier);

    if (Probability.getChance(2)) {
      change = change.negate();
    }

    return baseValue.add(change);
  }

  private int getNewStock(BigDecimal baseValue, int existingStock) {
    int adjustedBaseValue;
    try {
      adjustedBaseValue = baseValue.round(MathContext.DECIMAL64).intValueExact();
    } catch (ArithmeticException ex) {
      adjustedBaseValue = Integer.MAX_VALUE;
    }

    adjustedBaseValue = (int) Math.sqrt(adjustedBaseValue);
    adjustedBaseValue = Math.max(3, adjustedBaseValue);

    int changeUnit = Math.max(100, (Probability.getRangedRandom(20, 200) - adjustedBaseValue) * 100);
    int baseChange = Probability.getChance(32) ? Probability.getRandom(20) * changeUnit : changeUnit;
    int change = Probability.getRandom(Probability.getRandom(Math.max(1, baseChange - adjustedBaseValue)));

    if (Probability.getChance(adjustedBaseValue)) {
      existingStock += change;
    } else {
      existingStock -= change;
    }

    return Math.max(0, existingStock);
  }

  private void writeMarketJson() {
    Task.builder().execute(() -> {
      try (Connection con = SQLHandle.getConnection()) {
        DSLContext create = DSL.using(con);
        Result<Record3<String, BigDecimal, Integer>> results = create.select(ITEM_ALIASES.ALIAS, ITEM_DATA.CURRENT_VALUE, ITEM_DATA.STOCK)
            .from(ITEM_DATA).join(ITEM_ALIASES).on(ITEM_ALIASES.ITEM_ID.equal(ITEM_DATA.ID))
            .fetch();

        JsonArray items = new JsonArray();

        results.forEach((record) -> {
          JsonObject entry = new JsonObject();

          entry.addProperty("name", record.get(ITEM_ALIASES.ALIAS));
          entry.addProperty("buy_price", record.get(ITEM_DATA.CURRENT_VALUE));
          entry.addProperty("sell_price", record.get(ITEM_DATA.CURRENT_VALUE).multiply(getSellFactor(record.get(ITEM_DATA.CURRENT_VALUE))));
          entry.addProperty("stock", record.get(ITEM_DATA.STOCK));

          items.add(entry);
        });

        try (BufferedWriter writer = Files.newBufferedWriter(marketValuesPath)) {
          writer.write(items.toString());
        }
      } catch (SQLException | IOException e) {
        e.printStackTrace();
      }
    }).async().submit(SkreePlugin.inst());
  }

  @Override
  public void updatePrices() {
    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Result<Record3<Integer, BigDecimal, Integer>> results = create.select(ITEM_DATA.ID, ITEM_DATA.VALUE, ITEM_DATA.STOCK)
          .from(ITEM_DATA)
          .fetch();

      Collection<UpdateConditionStep<ItemDataRecord>> updates = new ArrayList<>();

      for (Record3<Integer, BigDecimal, Integer> result : results) {
        int itemId = result.getValue(ITEM_DATA.ID);
        BigDecimal baseValue = result.getValue(ITEM_DATA.VALUE);
        BigDecimal newValue = getNewValue(baseValue);
        int newStock = getNewStock(baseValue, result.getValue(ITEM_DATA.STOCK));

        updates.add(create.update(ITEM_DATA)
            .set(ITEM_DATA.CURRENT_VALUE, newValue)
            .set(ITEM_DATA.STOCK, newStock)
            .where(ITEM_DATA.ID.equal(itemId)));
      }

      create.batch(updates).execute();

      writeMarketJson();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Optional<Integer> getStock(String alias) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Record1<Integer> result = create.select(ITEM_DATA.STOCK).from(ITEM_DATA).where(
          ITEM_DATA.ID.equal(
              create.select(ITEM_ALIASES.ITEM_ID)
                  .from(ITEM_ALIASES)
                  .where(ITEM_ALIASES.ALIAS.equal(alias.toLowerCase()))
          )
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.getValue(ITEM_DATA.STOCK));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Optional<Integer> getStock(ItemStack stack) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      Record1<Integer> result = create.select(ITEM_DATA.STOCK).from(ITEM_DATA).where(
          ITEM_DATA.MC_ID.equal(idVariant.getKey()).and(ITEM_DATA.VARIANT.equal(idVariant.getValue()))
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.getValue(ITEM_DATA.STOCK));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public boolean setStock(String alias, int quantity) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      int changed = create.update(ITEM_DATA).set(ITEM_DATA.STOCK, quantity).where(
          ITEM_DATA.ID.equal(
              create.select(ITEM_ALIASES.ITEM_ID)
                  .from(ITEM_ALIASES).where(ITEM_ALIASES.ALIAS.equal(alias.toLowerCase()))
          )
      ).execute();

      writeMarketJson();

      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean setStock(ItemStack stack, int quantity) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      int changed = create.update(ITEM_DATA).set(ITEM_DATA.STOCK, quantity).where(
          ITEM_DATA.MC_ID.equal(idVariant.getKey()).and(ITEM_DATA.VARIANT.equal(idVariant.getValue()))
      ).execute();

      writeMarketJson();

      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public Optional<BigDecimal> getPrice(String alias) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Record1<BigDecimal> result = create.select(ITEM_DATA.CURRENT_VALUE).from(ITEM_DATA).where(
          ITEM_DATA.ID.equal(
              create.select(ITEM_ALIASES.ITEM_ID)
                  .from(ITEM_ALIASES)
                  .where(ITEM_ALIASES.ALIAS.equal(alias.toLowerCase()))
          )
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.getValue(ITEM_DATA.CURRENT_VALUE));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Optional<BigDecimal> getPrice(ItemStack stack) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      Record1<BigDecimal> result = create.select(ITEM_DATA.CURRENT_VALUE).from(ITEM_DATA).where(
          ITEM_DATA.MC_ID.equal(idVariant.getKey()).and(ITEM_DATA.VARIANT.equal(idVariant.getValue()))
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.getValue(ITEM_DATA.CURRENT_VALUE));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Optional<BigDecimal> getBasePrice(String alias) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Record1<BigDecimal> result = create.select(ITEM_DATA.VALUE).from(ITEM_DATA).where(
          ITEM_DATA.ID.equal(
              create.select(ITEM_ALIASES.ITEM_ID)
                  .from(ITEM_ALIASES)
                  .where(ITEM_ALIASES.ALIAS.equal(alias.toLowerCase()))
          )
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.getValue(ITEM_DATA.VALUE));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Optional<BigDecimal> getBasePrice(ItemStack stack) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      Record1<BigDecimal> result = create.select(ITEM_DATA.VALUE).from(ITEM_DATA).where(
          ITEM_DATA.MC_ID.equal(idVariant.getKey()).and(ITEM_DATA.VARIANT.equal(idVariant.getValue()))
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.getValue(ITEM_DATA.VALUE));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public boolean setBasePrice(String alias, BigDecimal price) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      int changed = create.update(ITEM_DATA)
          .set(ITEM_DATA.VALUE, price)
          .set(ITEM_DATA.CURRENT_VALUE, price)
          .where(ITEM_DATA.ID.equal(
              create.select(ITEM_ALIASES.ITEM_ID)
                  .from(ITEM_ALIASES).where(ITEM_ALIASES.ALIAS.equal(alias.toLowerCase()))
              )
          ).execute();

      writeMarketJson();

      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean setBasePrice(ItemStack stack, BigDecimal price) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      int changed = create.update(ITEM_DATA)
          .set(ITEM_DATA.VALUE, price)
          .set(ITEM_DATA.CURRENT_VALUE, price)
          .where(ITEM_DATA.MC_ID.equal(idVariant.getKey()).and(ITEM_DATA.VARIANT.equal(idVariant.getValue()))
          ).execute();

      writeMarketJson();

      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean addItem(ItemStack stack) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      int changed = create.insertInto(ITEM_DATA)
          .columns(ITEM_DATA.MC_ID, ITEM_DATA.VARIANT)
          .values(idVariant.getKey(), idVariant.getValue())
          .onDuplicateKeyIgnore()
          .execute();
      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean remItem(ItemStack stack) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      int changed = create.deleteFrom(ITEM_DATA)
          .where(ITEM_DATA.MC_ID.equal(idVariant.getKey()).and(ITEM_DATA.VARIANT.equal(idVariant.getValue())))
          .execute();
      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean setPrimaryAlias(String alias) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      int changed = create.update(ITEM_DATA).set(
          ITEM_DATA.PRIMARY_ALIAS,
          DSL.select(ITEM_ALIASES.ID).from(ITEM_ALIASES).where(
              ITEM_ALIASES.ALIAS.equal(alias.toLowerCase())
          )
      ).where(
          ITEM_DATA.ID.equal(
              DSL.select(ITEM_ALIASES.ITEM_ID).from(ITEM_ALIASES).where(
                  ITEM_ALIASES.ALIAS.equal(alias.toLowerCase())
              )
          )
      ).execute();

      writeMarketJson();

      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean addAlias(String alias, ItemStack stack) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      int changed = create.insertInto(ITEM_ALIASES)
          .columns(ITEM_ALIASES.ITEM_ID, ITEM_ALIASES.ALIAS)
          .select(create.select(ITEM_DATA.ID, DSL.val(alias.toLowerCase()))
              .from(ITEM_DATA)
              .where(ITEM_DATA.MC_ID.equal(idVariant.getKey())
                  .and(ITEM_DATA.VARIANT.equal(idVariant.getValue()))
              )
          ).onDuplicateKeyIgnore().execute();
      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean remAlias(String alias) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      int changed = create.deleteFrom(ITEM_ALIASES)
          .where(ITEM_ALIASES.ALIAS.equal(alias.toLowerCase()))
          .execute();
      return changed > 0;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public Optional<String> getAlias(String alias) {
    validateAlias(alias);

    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Record1<String> result = create.select(ITEM_ALIASES.ALIAS).from(ITEM_ALIASES).where(
          ITEM_ALIASES.ID.equal(
              DSL.select(ITEM_DATA.PRIMARY_ALIAS).from(ITEM_DATA).where(
                  ITEM_DATA.ID.equal(
                      DSL.select(ITEM_ALIASES.ITEM_ID).from(ITEM_ALIASES).where(
                          ITEM_ALIASES.ALIAS.equal(alias.toLowerCase())
                      )
                  )
              )
          )
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.value1());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> getAlias(ItemStack stack) {
    try (Connection con = SQLHandle.getConnection()) {
      Clause<String, String> idVariant = getIDVariant(stack);

      DSLContext create = DSL.using(con);
      Record1<String> result = create.select(ITEM_ALIASES.ALIAS).from(ITEM_ALIASES).where(
          ITEM_ALIASES.ID.equal(
              DSL.select(ITEM_DATA.PRIMARY_ALIAS).from(ITEM_DATA).where(
                  ITEM_DATA.MC_ID.equal(idVariant.getKey())
                      .and(ITEM_DATA.VARIANT.equal(idVariant.getValue())))
          )
      ).fetchOne();
      return result == null ? Optional.empty() : Optional.of(result.value1());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public List<ItemDescriptor> getPrices() {
    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Result<Record3<String, BigDecimal, Integer>> result = create.select(ITEM_ALIASES.ALIAS, ITEM_DATA.CURRENT_VALUE, ITEM_DATA.STOCK)
          .from(ITEM_DATA, ITEM_ALIASES)
          .where(ITEM_DATA.PRIMARY_ALIAS.equal(ITEM_ALIASES.ID))
          .fetch();

      return result.stream().map(entry -> new ItemDescriptor(entry.value1(), entry.value2(), entry.value3())).collect(Collectors.toList());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return new ArrayList<>();
  }

  @Override
  public List<ItemDescriptor> getPrices(String aliasConstraint) {
    try (Connection con = SQLHandle.getConnection()) {
      DSLContext create = DSL.using(con);
      Result<Record3<String, BigDecimal, Integer>> result = create.select(ITEM_ALIASES.ALIAS, ITEM_DATA.CURRENT_VALUE, ITEM_DATA.STOCK)
          .from(ITEM_DATA, ITEM_ALIASES)
          .where(ITEM_DATA.PRIMARY_ALIAS.equal(ITEM_ALIASES.ID)).and(ITEM_ALIASES.ALIAS.like(aliasConstraint))
          .fetch();

      return result.stream().map(entry -> new ItemDescriptor(entry.value1(), entry.value2(), entry.value3())).collect(Collectors.toList());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return new ArrayList<>();
  }

  private <R, T> Map<T, Integer> condenseCount(Collection<Clause<R, Integer>> input, Function<R, T> remap) {
    Map<T, Integer> count = new HashMap<>();
    for (Clause<R, Integer> entry : input) {
      count.merge(remap.apply(entry.getKey()), entry.getValue(), (a, b) -> a + b);
    }
    return count;
  }

  @Override
  public boolean logTransactionByName(UUID user, Collection<Clause<String, Integer>> itemQuantity) {
    // Map<String, Integer> aliasCount = condenseCount(itemQuantity, (a) -> a);
    return true;
  }

  @Override
  public boolean logTransactionByStack(UUID user, Collection<Clause<ItemStack, Integer>> itemQuantity) {
    // Map<Clause<String, String>, Integer> itemCount = condenseCount(itemQuantity, this::getIDVariant);
    return true;
  }

  private static Map<String, TypeDeducer> varientResolutionMap = new HashMap<>();

  private TypeDeducer getDeducer(String type) {
    return varientResolutionMap.getOrDefault(type, new DeducerOfSimpleType());
  }

  private ItemStack getItemStack(Clause<String, String> idVariant) {
    return getDeducer(idVariant.getKey()).getItemStack(idVariant);
  }

  private Clause<String, String> getIDVariant(ItemStack stack) {
    return getDeducer(stack.getItem().getId()).getVariant(stack);
  }
}
