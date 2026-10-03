# QQAssistant

Chat assistant bot for a Minecraft server. Listens to game chat (including via Chatty and multiple chats/channels), finds the target by mention, and replies according to configurable rules — with delays, condition checks, random answers, and step-by-step player survey "sessions".

GitHub Actions automatically builds the ready-to-use `QQAssistant.jar` — just push the repository.

---

## Quick navigation

<kbd><a href="#install">Installation</a></kbd>
<kbd><a href="#requirements">Requirements</a></kbd>
<kbd><a href="#commands">Commands</a></kbd>
<kbd><a href="#permissions">Permissions</a></kbd>
<kbd><a href="#config">Configuration</a></kbd>
<kbd><a href="#actions">Action types</a></kbd>
<kbd><a href="#placeholders">Placeholders</a></kbd>
<kbd><a href="#build">Build</a></kbd>
<kbd><a href="#faq">FAQ</a></kbd>
<kbd><a href="#license">License</a></kbd>

---

<a id="features"></a>
## Features

- Replies to messages by exact/contains/regex match
- Rules with priority, trigger chance, and cooldown fallback
- Target detection by mention (including offline) and placeholder substitution
- Reply delay, titles, actionbars, sounds, executing commands as console/player
- Arguments from the message (`args-def`) and their subsequent validation in actions (`arg:`)
- Step-by-step survey sessions (`session`) with timeouts, skip, and cancel
- Support for multiple chats via Chatty (mandatory primary path) + fallback path `AsyncPlayerChatEvent`
- Full PlaceholderAPI integration

<a id="requirements"></a>
## Requirements

- Paper-based server **1.21+** (Java **21**)
- **PlaceholderAPI** — required (`depend`)
- [**Chatty**](https://github.com/Brikster/Chatty) — recommended (`softdepend`), needed for receiving messages from multiple chats/channels

<a id="install"></a>
## Installation

1. Install **PlaceholderAPI** (and **Chatty**, if you need multiple chats) on the server.
2. Download the built `QQAssistant.jar` from the **Actions** tab → the latest artifact of this repository.
3. Place the file into the server's `plugins/` folder.
4. Restart the server (or `/reload`).
5. On first launch, `plugins/QQAssistant/config.yml` will be created — open it and configure the rules to your liking, then run `/qqassistant reload`.

> Note: `config.yml` is copied with the default one only on first launch. If the file already exists — edit it directly.

If **Chatty** is installed on the server, make sure the chat is actually handled by Chatty (it has its own global chat and other channels). QQAssistant listens to `ChattyMessageEvent` and processes messages on the main thread — the Bukkit API is not called from an async thread.

<a id="commands"></a>
## Commands

| Command | Description |
|---|---|
| `/qqassistant` | List of plugin information |
| `/qqassistant reload` | Reload configuration on the fly |
| `/qqassistant info` | Output an informational message |

Command aliases: `qqa`, `qqassist`, `assist`, `assistant`, `bot`, `chatbot`.

<a id="permissions"></a>
## Permissions

| Permission | Description | Default |
|---|---|---|
| `qqassist.admin` | Access to admin commands | op |

<a id="config"></a>
## Configuration

Main top-level sections:

- `aliases` — command aliases (default `qqa`, `qqassist`, `assist`, `assistant`, `bot`, `chatbot`)
- `mention-patterns` — player mention patterns, `{player}` is substituted as the name (e.g. `@?{player}`)
- `settings` — `prefix`, `debug` (event logging), `session-timeout`, `actionbar-refresh-ticks`, `placeholder-defaults`
- `messages` — plugin message texts (`no_permission`, `reload_success`, `unknown_command`, `info_message`)
- `rules` — reply rules

### Rule (`rules.<name>`)

| Field | Description |
|---|---|
| `allowed-chats` | Only these chats (id from Chatty). Empty — all |
| `permission` | Required player permission. Empty — no check |
| `condition` | Additional condition (PAPI strings) |
| `priority` | Higher — processed earlier |
| `chance` | Trigger chance, % |
| `cooldown_ticks` | Cooldown for the player |
| `delay_ticks` | Reply delay, ticks |
| `questions.exact` / `contains` / `regex` | Match arrays |
| `answers` / `random_answers` | Answers (actions). `random_answers` — random from the list |
| `args-def` | Named arguments extracted from the message text |
| `session` | Step-by-step session settings: `enabled`, `steps` (id/prompt/validate/error/default), `timeout`, `idle-timeout`, `idle-message`, `cancel-message`, `cancel-triggers`, `skip-triggers`, `listen-chat`, `args-timeout` |

### Session

Session questions are asked one step at a time, the player's answer is validated by the `validate` regex field. Skipping a step (if a `default` exists) — via `skip-triggers`, cancelling — via `cancel-triggers`. The player can give a timeout (`wait 5 minutes` / `подожди 5 минут`) — the session does not close, but reminds of the question.

<a id="actions"></a>
## Action types

Each element of `answers`/`random_answers`/`session.*` can be `type!value`:

| Type | Example | Description |
|---|---|---|
| `message!` | `message! Hello, %qqassist_target%!` | Private message to the player (with its own colors/gradients) |
| `gMessage!` | `gMessage! This is for everyone` | Broadcast message |
| `title!` | `title!Title` | Title (fadeIn 20, stay 40, fadeOut 20) |
| `title:` | `title:5:20:10!Text` | Title with custom timings |
| `actionbar!` | `actionbar! Text` | Actionbar (60 ticks) |
| `actionbar:` | `actionbar:100!Text` | Actionbar with duration |
| `sound!` | `sound! ENTITY_ENDERMAN_TELEPORT 1.0 1.0` | Sound to the player |
| `gSound!` | `gSound! BLOCK_NOTE_PLING 1.0 2.0` | Sound to everyone |
| `asConsole!` | `asConsole! say from the bot` | Command as console |
| `asPlayer!` | `asPlayer! spawn` | Command as player |
| `delay:` | `delay:40! message! Later` | Execute the next action with a delay |
| `arg:` | `arg:1[options]!...` | Conditional execution by argument: `*`, `regex:`, `papi:`, `contains:`, list via `\|\|` |
| `check:[...]` | `check:[%some% == 5]!message! There is 5` | Execution by condition |

Inside conditions, the following operators are available: `=`, `!=`, `>`, `<`, `>=`, `<=`, `<-` (contains), `!<-`, `|-` (starts with), `!|-`, `-|` (ends with), `!-|`.

<a id="placeholders"></a>
## Placeholders

Everything — via `%qqassist_...%` (Player-local, also works in other PAPI plugins):

| Placeholder | Description |
|---|---|
| `%qqassist_prefix%` | Plugin prefix |
| `%qqassist_message%` | Player's last message |
| `%qqassist_target%` | Target name (by mention) |
| `%qqassist_random_target%` | Random known player |
| `%qqassist_random_online_target%` | Random online player |
| `%qqassist_session_target%` / `%qqassist_session_current_step%` / `%qqassist_session_idle_left%` | Active session data |
| `%qqassist_session_arg_<name>%` | Session argument by step name |
| `%qqassist_arg_<N>%` | Rule argument by number (1-based) |
| `%qqassist_parse_{placeholder}%` | Value of an arbitrary placeholder for the target (or the source player, if there is no target) |
| `%qqassist_target_uuid%`, `%qqassist_target_world%`, `%qqassist_target_health%`, `%qqassist_target_max_health%`, `%qqassist_target_level%`, `%qqassist_target_gamemode%`, `%qqassist_target_food%`, `%qqassist_target_xp%` | Target characteristics |

<a id="build"></a>
## Build

### Via GitHub Actions (recommended)

1. Create a repository on GitHub and push this project.
2. Open the **Actions** tab — the `Build` workflow will start automatically.
3. In the latest completed run, download the **QQAssistant** artifact — that is the ready-to-use `QQAssistant.jar`.

The build uses JDK 21: `mvn -B -ntp package` → `target/QQAssistant.jar` (a single file, without a version in the name and without `original-`).

### Locally

Requires JDK 21 and Maven:

```
mvn package
```

Ready plugin: `target/QQAssistant.jar`

<a id="faq"></a>
## FAQ

**The bot does not reply to messages.**

1. Enable `settings.debug: true` in `plugins/QQAssistant/config.yml` and run `/qqassistant reload`. Lines like `[QQAssistant] [Chatty] chatId=... msg=...` will appear in the console.
2. Check that Chatty is handling the chat (if Chatty is installed, the fallback `AsyncPlayerChatEvent` intentionally stays silent).
3. Make sure the rule passes the filters: `allowed-chats`, `permission`, `cooldown_ticks`, `chance`, `priority`. Rules with `regex` like `.*` will match everything — debug in the order of the `rules` section.
4. Check that the player has the permission from the rule's `permission` field.
5. If there is not a single `[QQAssistant]` line in the console — the time of the player's question exceeds the 500 ms cooldown inside `processMessage` (double-trigger protection).

<a id="license"></a>
## License

The project is distributed under the **MIT** license. Author — AllF1RE.
