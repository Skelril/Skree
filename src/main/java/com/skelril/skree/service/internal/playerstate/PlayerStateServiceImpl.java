/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service.internal.playerstate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.skelril.nitro.entity.EntityHealthUtil;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.service.PlayerStateService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.skelril.nitro.item.ItemSerializer.deserializeItemStack;
import static com.skelril.nitro.item.ItemSerializer.serializeItemStack;
import static com.skelril.nitro.item.ItemStackFactory.newItemStack;

public class PlayerStateServiceImpl implements PlayerStateService {

  private static final String GENERAL_STORE_NAME = "general_store";

  private Path getFile(Player player) throws IOException {
    ConfigManager service = Sponge.getGame().getConfigManager();
    Path path = service.getPluginConfig(SkreePlugin.inst()).getDirectory();
    path = Files.createDirectories(path.resolve("profiles"));
    return path.resolve(player.getUniqueId() + ".json");
  }

  private Optional<SavedPlayerStateContainer> getContainer(Player player) throws IOException {
    if (!getFile(player).toFile().exists()) {
      return Optional.empty();
    }

    try (BufferedReader reader = Files.newBufferedReader(getFile(player))) {
      Gson gson = new GsonBuilder().create();
      return Optional.of(gson.fromJson(reader, SavedPlayerStateContainer.class));
    }
  }

  private void writeContainer(Player player, SavedPlayerStateContainer container) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(getFile(player))) {
      Gson gson = new GsonBuilder().create();
      writer.write(gson.toJson(container));
    }
  }

  @Override
  public boolean hasInventoryStored(Player player) {
    try {
      SavedPlayerStateContainer stateContainer = getContainer(player).orElse(new SavedPlayerStateContainer());
      Map<String, SavedPlayerState> states = stateContainer.getSavedPlayerStates();

      return states.get(GENERAL_STORE_NAME) != null;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean hasReleasedInventoryStored(Player player) {
    try {
      SavedPlayerStateContainer container = getContainer(player).orElse(new SavedPlayerStateContainer());
      SavedPlayerState playerState = container.getSavedPlayerStates().get(container.getReleasedState());
      return playerState != null;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public void storeInventory(Player player) throws InventoryStorageStateException {
    if (hasInventoryStored(player)) {
      throw new InventoryStorageStateException();
    }

    save(player, GENERAL_STORE_NAME);
  }

  @Override
  public void loadInventory(Player player) throws InventoryStorageStateException {
    if (!hasInventoryStored(player)) {
      throw new InventoryStorageStateException();
    }

    load(player, GENERAL_STORE_NAME);
    destroySave(player, GENERAL_STORE_NAME);
  }

  @Override
  public void releaseInventory(Player player) throws InventoryStorageStateException {
    if (!hasInventoryStored(player)) {
      throw new InventoryStorageStateException();
    }

    try {
      SavedPlayerStateContainer container = getContainer(player).orElse(new SavedPlayerStateContainer());
      container.setReleasedState(GENERAL_STORE_NAME);

      writeContainer(player, container);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private SavedPlayerState createNewPlayerState(Player player) {
    SavedPlayerState playerState = new SavedPlayerState();

    player.getInventory().slots().forEach(slot -> {
      try {
        ItemStack stackInSlot = slot.peek().orElse(newItemStack(ItemTypes.NONE));
        JsonElement serializedStack = serializeItemStack(stackInSlot);
        playerState.getInventoryContents().add(serializedStack);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    return playerState;
  }

  @Override
  public void save(Player player, String saveName) {
    try {
      SavedPlayerStateContainer stateContainer = getContainer(player).orElse(new SavedPlayerStateContainer());
      Map<String, SavedPlayerState> states = stateContainer.getSavedPlayerStates();
      states.put(saveName, createNewPlayerState(player));

      writeContainer(player, stateContainer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private List<JsonElement> getInventoryContents(Player player, String saveName) {
    try {
      SavedPlayerStateContainer stateContainer = getContainer(player).orElse(new SavedPlayerStateContainer());
      SavedPlayerState state = stateContainer.getSavedPlayerStates().getOrDefault(saveName, new SavedPlayerState());
      return state.getInventoryContents();
    } catch (IOException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  @Override
  public void load(Player player, String saveName) {
    Iterator<Inventory> slots = player.getInventory().slots().iterator();
    List<JsonElement> persistedInventoryContents = getInventoryContents(player, saveName);
    for (int i = 0; slots.hasNext(); ++i) {
      Inventory slot = slots.next();
      if (i < persistedInventoryContents.size()) {
        try {
          ItemStack stack = deserializeItemStack(persistedInventoryContents.get(i));
          slot.set(stack);
          continue;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      slot.set(newItemStack(ItemTypes.NONE));
    }
  }

  private void destroySave(Player player, String saveName) {
    try {
      SavedPlayerStateContainer container = getContainer(player).orElse(new SavedPlayerStateContainer());
      container.getSavedPlayerStates().remove(saveName);
      if (Objects.equals(saveName, GENERAL_STORE_NAME)) {
        container.setReleasedState(null);
      }

      writeContainer(player, container);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Listener(order = Order.LAST)
  public void onPlayerRespawn(RespawnPlayerEvent event) {
    Player player = event.getTargetEntity();
    if (hasReleasedInventoryStored(player)) {
      try {
        loadInventory(player);
      } catch (InventoryStorageStateException e) {
        e.printStackTrace();
      }
    }
  }

  @Listener(order = Order.LAST)
  public void onPlayerLogin(ClientConnectionEvent.Join event) {
    Player player = event.getTargetEntity();
    if (hasReleasedInventoryStored(player) && EntityHealthUtil.getHealth(player) > 0) {
      try {
        loadInventory(player);
      } catch (InventoryStorageStateException e) {
        e.printStackTrace();
      }
    }
  }
}
