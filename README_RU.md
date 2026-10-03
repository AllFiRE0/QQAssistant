# QQAssistant

Чат-бот ассистент для Minecraft-сервера. Слушает игровой чат (в том числе через Chatty и несколько чатов/каналов), находит цель по упоминанию и отвечает по настраиваемым правилам — с задержками, проверкой условий, случайными ответами и пошаговыми «сессиями» опроса игрока.

GitHub Actions автоматически собирает готовый `QQAssistant.jar` — просто запуши репозиторий.

---

## Быстрая навигация

<kbd><a href="#install">Установка</a></kbd>
<kbd><a href="#requirements">Требования</a></kbd>
<kbd><a href="#commands">Команды</a></kbd>
<kbd><a href="#permissions">Разрешения</a></kbd>
<kbd><a href="#config">Конфигурация</a></kbd>
<kbd><a href="#actions">Типы действий</a></kbd>
<kbd><a href="#placeholders">Плейсхолдеры</a></kbd>
<kbd><a href="#build">Сборка</a></kbd>
<kbd><a href="#faq">FAQ</a></kbd>
<kbd><a href="#license">Лицензия</a></kbd>

---

<a id="features"></a>
## Возможности

- Ответы на сообщения по точному/содержательному/регулярному совпадению
- Правила с приоритетом, шансом срабатывания и откатом кулдауна
- Определение цели по упоминанию (в том числе оффлайн) и замена плейсхолдеров
- Задержка ответа, титулы, экшнбары, звуки, выполнение команд от консоли/игрока
- Аргументы из сообщения (`args-def`) и их последующая проверка в действиях (`arg:`)
- Пошаговые сессии опроса (`session`) с тайм-аутами, пропуском и отменой
- Поддержка нескольких чатов через Chatty (обязательный основной путь) + запасной путь `AsyncPlayerChatEvent`
- Полная интеграция с PlaceholderAPI

<a id="requirements"></a>
## Требования

- Сервер на базе Paper **1.21+** (Java **21**)
- **PlaceholderAPI** — обязателен (`depend`)
- [**Chatty**](https://github.com/Brikster/Chatty) — рекомендуется (`softdepend`), нужен для приёма сообщений из нескольких чатов/каналов

<a id="install"></a>
## Установка

1. Установи **PlaceholderAPI** (и **Chatty**, если нужно несколько чатов) на сервер.
2. Скачай собранный `QQAssistant.jar` из вкладки **Actions** → последний артефакт этого репозитория.
3. Положи файл в папку `plugins/` сервера.
4. Перезапусти сервер (или `/reload`).
5. При первом запуске создастся `plugins/QQAssistant/config.yml` — открой и настрой правила под себя, затем выполни `/qqassistant reload`.

> Внимание: `config.yml` копируется с дефолтным только при первом запуске. Если файл уже существует — правь его напрямую.

Если на сервере установлен **Chatty**, убедись, что чат действительно обрабатывается Chatty (у него есть свой глобальный чат и другие каналы). QQAssistant слушает `ChattyMessageEvent` и обрабатывает сообщения на главном потоке — Bukkit API не вызывается из async-потока.

<a id="commands"></a>
## Команды

| Команда | Описание |
|---|---|
| `/qqassistant` | Список информации о плагине |
| `/qqassistant reload` | Перезагрузка конфигурации на лету |
| `/qqassistant info` | Вывод информационного сообщения |

Алиасы команды: `qqa`, `qqassist`, `assist`, `assistant`, `bot`, `chatbot`.

<a id="permissions"></a>
## Разрешения

| Разрешение | Описание | По умолчанию |
|---|---|---|
| `qqassist.admin` | Доступ к админ-командам | op |

<a id="config"></a>
## Конфигурация

Основные секции верхнего уровня:

- `aliases` — алиасы команды (по умолчанию `qqa`, `qqassist`, `assist`, `assistant`, `bot`, `chatbot`)
- `mention-patterns` — шаблоны упоминания игрока, `{player}` подставляется как имя (например `@?{player}`)
- `settings` — `prefix`, `debug` (лог событий), `session-timeout`, `actionbar-refresh-ticks`, `placeholder-defaults`
- `messages` — тексты сообщений плагина (`no_permission`, `reload_success`, `unknown_command`, `info_message`)
- `rules` — правила ответов

### Правило (`rules.<имя>`)

| Поле | Описание |
|---|---|
| `allowed-chats` | Только эти чаты (id из Chatty). Пусто — все |
| `permission` | Требуемое право игрока. Пусто — без проверки |
| `condition` | Дополнительное условие (строки PAPI) |
| `priority` | Выше — обрабатывается раньше |
| `chance` | Шанс срабатывания, % |
| `cooldown_ticks` | Кулдаун для игрока |
| `delay_ticks` | Задержка ответа, тики |
| `questions.exact` / `contains` / `regex` | Массивы совпадений |
| `answers` / `random_answers` | Ответы (действия). `random_answers` — случайный из списка |
| `args-def` | Именованные аргументы, извлекаемые из текста сообщения |
| `session` | Настройки пошаговой сессии: `enabled`, `steps` (id/prompt/validate/error/default), `timeout`, `idle-timeout`, `idle-message`, `cancel-message`, `cancel-triggers`, `skip-triggers`, `listen-chat`, `args-timeout` |

### Сессия

Вопросы сессии задаются по одному шагу, ответ игрока проверяется regex-ом поля `validate`. Пропуск шага (если есть `default`) — через `skip-triggers`, отмена — через `cancel-triggers`. Игрок может дать тайм-аут (`подожди 5 минут` / `wait 5 min`) — сессия не закрывается, а напоминает вопрос.

<a id="actions"></a>
## Типы действий

Каждый элемент `answers`/`random_answers`/`session.*` может быть `тип!значение`:

| Тип | Пример | Описание |
|---|---|---|
| `message!` | `message! Привет, %qqassist_target%!` | ЛС игроку (со своими цветами/градиентами) |
| `gMessage!` | `gMessage! Это всем` | Широковещательное сообщение |
| `title!` | `title!Заголовок` | Тайтл (fadeIn 20, stay 40, fadeOut 20) |
| `title:` | `title:5:20:10!Текст` | Тайтл со своими таймингами |
| `actionbar!` | `actionbar! Текст` | Экшнбар (60 тиков) |
| `actionbar:` | `actionbar:100!Текст` | Экшнбар с длительностью |
| `sound!` | `sound! ENTITY_ENDERMAN_TELEPORT 1.0 1.0` | Звук игроку |
| `gSound!` | `gSound! BLOCK_NOTE_PLING 1.0 2.0` | Звук всем |
| `asConsole!` | `asConsole! say от бота` | Команда от консоли |
| `asPlayer!` | `asPlayer! spawn` | Команда от игрока |
| `delay:` | `delay:40! message! Позже` | Выполнение следующего действия с задержкой |
| `arg:` | `arg:1[варианты]!...` | Условное выполнение по аргументу: `*`, `regex:`, `papi:`, `contains:`, список через `\|\|` |
| `check:[...]` | `check:[%some% == 5]!message! Есть 5` | Выполнение по условию |

Внутри условий доступны операторы: `=`, `!=`, `>`, `<`, `>=`, `<=`, `<-` (содержит), `!<-`, `|-` (начинается), `!|-`, `-|` (заканчивается), `!-|`.

<a id="placeholders"></a>
## Плейсхолдеры

Всё — через `%qqassist_...%` (Player-local, работает и в других плагинах с PAPI):

| Плейсхолдер | Описание |
|---|---|
| `%qqassist_prefix%` | Префикс плагина |
| `%qqassist_message%` | Последнее сообщение игрока |
| `%qqassist_target%` | Имя цели (по упоминанию) |
| `%qqassist_random_target%` | Случайный известный игрок |
| `%qqassist_random_online_target%` | Случайный онлайн-игрок |
| `%qqassist_session_target%` / `%qqassist_session_current_step%` / `%qqassist_session_idle_left%` | Данные активной сессии |
| `%qqassist_session_arg_<имя>%` | Аргумент сессии по имени шага |
| `%qqassist_arg_<N>%` | Аргумент правила по номеру (1-based) |
| `%qqassist_parse_{placeholder}%` | Значение произвольного плейсхолдера у цели (или исходного игрока, если цели нет) |
| `%qqassist_target_uuid%`, `%qqassist_target_world%`, `%qqassist_target_health%`, `%qqassist_target_max_health%`, `%qqassist_target_level%`, `%qqassist_target_gamemode%`, `%qqassist_target_food%`, `%qqassist_target_xp%` | Характеристики цели |

<a id="build"></a>
## Сборка

### Через GitHub Actions (рекомендуется)

1. Создай репозиторий на GitHub и запуши этот проект.
2. Открой вкладку **Actions** — workflow `Build` запустится автоматически.
3. В последнем выполненном прогоне скачай артефакт **QQAssistant** — это и есть готовый `QQAssistant.jar`.

Сборка использует JDK 21: `mvn -B -ntp package` → `target/QQAssistant.jar` (один файл, без версии в имени и без `original-`).

### Локально

Требуется JDK 21 и Maven:

```
mvn package
```

Готовый плагин: `target/QQAssistant.jar`

<a id="faq"></a>
## FAQ

**Бот не отвечает на сообщения.**

1. Включи `settings.debug: true` в `plugins/QQAssistant/config.yml` и выполни `/qqassistant reload`. В консоли появятся строки `[QQAssistant] [Chatty] chatId=... msg=...`.
2. Проверь, что Chatty обрабатывает чат (если Chatty установлен, запасной `AsyncPlayerChatEvent` молчит намеренно).
3. Убедись, что правило проходит фильтры: `allowed-chats`, `permission`, `cooldown_ticks`, `chance`, `priority`. Правила с `regex` вида `.*` подойдут под всё — дебажь по порядку секции `rules`.
4. Проверь, что у игрока есть право из поля `permission` правила.
5. Если в консоли нет ни одной строки `[QQAssistant]` — время вопроса игроком превышает кулдаун в 500 мс внутри `processMessage` (защита от двойного срабатывания).

<a id="license"></a>
## Лицензия

Проект распространяется под лицензией **MIT**. Автор — AllF1RE.
