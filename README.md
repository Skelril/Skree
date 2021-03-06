## General Project Structure

* Nitro
  - Packages for various Nitro general purpose MC libraries
* Skree
  - Content
    * Packages which represent game content, and things which are server specific. Anything which is used for a game mechanic, but otherwise not meaningful to a service should go here.
  - Service
    * Packages which define services, and their respective inner workings. Services should have one interface, and have their internal contents packaged inside a subpackage of service called 'internal.servicename'.
  - System
    * The initialization system, this is the part which initializes services, and uses them to create content from the content package.

## Building

### Initial

To build the project you should use the following commands:

1. ```gradle setupDecompWorkspace```
2. ```gradle build```

### Incremental

After initial setup you should be able to simply use ```gradle build```.

Note: If our Forge dependency has been updated, you may have to use ```gradle setupDecompWorkspace --refresh-dependencies``` to refresh your deobfuscated Minecraft sources.

## Database / jOOQ Schema Generation

The jOOQ Schema is generated via the gradle task ````codegen````. This task should only be run if you have a mariadb server setup locally running the proper database configuration.

Database development which does not require schema modification can be performed by simply using the generated jOOQ files. These are updated as the server's active schema is changed.

### Setting up MariaDB

First setup a MariaDB server on your development machine. Once that is completed,
log in as root, and run the following SQL statements:

```
CREATE SCHEMA mc_db;
CREATE USER 'mc_db_dev'@'localhost' IDENTIFIED BY 'mc_db_dev';
GRANT ALL PRIVILEGES ON *.* TO 'mc_db_dev'@'localhost';
FLUSH PRIVILEGES;
```