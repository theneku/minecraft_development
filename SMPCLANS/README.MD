# LeafMC - Clan Plugin for the SMP

This is a simple clan plugin for Paper/Spigot servers, developed to offer a lightweight, intuitive, and easily expandable clan system.  
> [!DISCLAIMER]  
> This plugin is NOT in its final version. Numerous implementations and improvements are still planned. As it is easily noticeable, it will be integrated into the core of the SMP, therefore some features that can be integrated with Vault, LuckPerms, etc., have not yet been implemented.

## Features

* Clan creation and deletion, invites, member management (including promotion to Co-Leader and member kicking)
* Interactive GUIs for the top 3 clans (naturally expandable to a leaderboard of all clans)
* Integrated database, in-memory cache, async support to prevent server lag
* Messages with Adventure API (runnable on Paper). N.B.: Java 17+
  

| Command                  | Description              |
| ------------------------ | ------------------------ |
| `/clan info`             | Shows clan information   |
| `/clan best`             | Leaderboard of top clans |
| `/clan chat`             | Clan chat                |
| `/clan create <name>`    | Creates a clan           |
| `/clan invite <player>`  | Invites a player         |
| `/clan accept <player>`  | Accepts an invitation    |
| `/clan leave`            | Leaves a clan            |
| `/clan kick <player>`    | Kicks a member           |
| `/clan promote <player>` | Promotes a member        |
| `/clan ally <clan>`      | Alliance with other clans|
| `/clan delete`           | Deletes the clan         |
  
**LIST OF USED APIs:**
* Paper API and Adventure API
* JDBC and CompletableFuture
* SQLite / MySQL 

## Possible Future Implementations (Nothing is set in stone)

* Clan war system
* Clan levels & XP
* PlaceholderAPI support
* Advanced permissions
* GUI configurations
* Full MySQL storage (IF NEEDED)
* Developer API (IF NEEDED)

[![Watch the video](https://youtu.be/Z0uHukbDqVs)](https://youtu.be/Z0uHukbDqVs)

[<img src="https://youtu.be/Z0uHukbDqVs" width="600" height="300"
/>](https://youtu.be/Z0uHukbDqVs)


## README

This project is currently distributed for development/testing purposes. As always, I recommend reading, understanding, and learning, rather than just skidding without awareness.
  
@theneku ~ eu@stardustsquad
