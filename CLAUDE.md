# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

Pusa Chat: a lightweight, all in memory, multi-user web chat. Runs inside Jetty 11 +
Curacao 7.2.1 following the same bootstrap pattern as `~/workspace/onyx` and
`~/workspace/jasper`. Java 21, single fat JAR. Originally (2012-2014) a Spring 3 MVC + JSP app
built with SBT; the front-end JavaScript (jQuery 1.7) and the API it calls are unchanged from
then, apart from restyling to pass ESLint.

## Build & Run Commands

```bash
mvn package                       # → dist/pusa-chat-1.0-runnable.jar (gulp, tests, checkstyle, PMD)
mvn test                          # the end-to-end test boots the full stack on an ephemeral port
java -Dconfig.file=$HOME/pusachat-dev.conf -jar dist/pusa-chat-1.0-runnable.jar   # http://127.0.0.1:8080/pusa-chat/
(cd src/main/webapp/static && node/node node_modules/gulp/bin/gulp.js dev)   # rebuild build/ on change
```

## Frontend Build

Same pipeline as onyx: a Gulp 5 build run by `frontend-maven-plugin` 2.0.2, in
`src/main/webapp/static`.
- Maven installs Node v24.21.0 / npm 11.19.0 into `static/node/` and runs `npm ci` (initialize),
  then `gulp release` (generate-resources). The system `node` is too old; use `node/node`.
- `release`: ESLint on `js/pusachat/**/*.js` (config `eslint.config.js`, onyx's rules) → concat
  to `build/app.{js,css}` → terser / clean-css to `release/app.min.{js,css}` → copyright banner.
- `layout.ftl` and `error.ftl` load `build/` in dev mode and `release/` otherwise; only
  `static/release/**` and `static/img/**` are copied into the JAR.
- The bundle order is the list in `gulpfile.js` (vendor libs, then `pusachat.js`, which defines
  the `PusaChat` namespace, then `app/chat.js` and `app/homepage.js`). New scripts must be added
  there.

## Architecture

- `Application` starts Jetty (context path from `pusa-chat.context-path`, default `/pusa-chat`);
  `CuracaoJakartaContextListener` then builds every `@Component` under the `pusachat`
  boot-package. Static files are served by Jetty's `DefaultServlet` from `/static/*`,
  `/favicon.ico` and `/robots.txt`; everything else goes to Curacao.
- `components/chat/ChatRooms` holds every room in memory, by ID and by name, and runs the
  thread that removes clients that stop polling. `ChatSessions` is what a client does in a
  room (register, message, delete, typing, inactivity, wait for the next event). Rooms are
  never removed. `ChatRoom.postEvent` fans an event out to every client's queue (and the
  room's log) on a shared pool, through a per-room Guava sequential executor: a room's events
  are delivered one at a time in the order they were posted, and an idle room holds no
  thread. Don't post to client queues any other way, or ordering breaks. `TokenSigner` signs
  room and client tokens (HMAC-SHA256, random key per boot).
- Events are delivered by long polling: the room page keeps one `GET /api/chat/event/{token}.json`
  open, which waits up to `pusa-chat.long-poll-timeout` (20s) and returns a NOOP if nothing
  happened. Each waiting client holds one Curacao request thread for that long, so
  `curacao.thread-pool.size` is roughly the number of clients the server can hold, and
  `curacao.async-context-timeout` must be longer than the long poll timeout.
- `controllers/Index` (homepage), `Room` (room page, find/create a room) and `Chat` (the chat
  API). API paths end in `.json`, as they did under Spring; the page JavaScript depends on the
  exact URLs and JSON field names.
- JSON is Jackson. The shared `ObjectMapper` is the `components/jackson/PusaChatJacksonObjectMapper`
  component (as `OnyxJacksonObjectMapper` in onyx); inject it, never build another. Every API
  entity implements the `entities/PusaChatEntity` interface, controllers return entities
  directly, and `mappers/response/PusaChatEntityResponseMapper` renders them with that
  mapper. `ExceptionResponseMapper` renders `ApiErrorResponse` the same way, through
  `PusaChatJsonEntity`.
- Events (`entities/events`) follow onyx's `entities/api/response/v1` pattern: each is an
  interface of `@JsonProperty` getters (its `type` a default method) with a nested `Builder`
  whose `build()` returns an anonymous implementation, e.g.
  `new Message.Builder().setFrom(...).setHtml(...).build()`. Unlike onyx's, the builders take no
  `ObjectMapper` (entities don't render themselves). Every builder extends
  `PusaChatEvent.AbstractPusaChatEventBuilder`, which gives the event a new `id` and its `when`
  (to the millisecond). Events have no `equals`; compare them by `getId()`.
- Pages are FreeMarker via curacao-freemarker, as in onyx and jasper: templates in
  `src/main/webapp/templates` (`layout.ftl` is the shared frame), started from
  `components/freemarker/PusaChatFreeMarkerContent` (`newHtmlBuilder(...)`, which adds the
  `HtmlMinifier` + `HtmlCompressor` transformers). `FreeMarkerContentRenderer` adds the
  attributes every page gets (`contextPath`, `devMode`, `buildVersion`).
- `mappers/response/exceptions/ExceptionResponseMapper` renders anything thrown: a JSON error
  for paths ending in `.json`, the `error.ftl` page otherwise. Every app exception extends
  `PusaChatException`, a `CuracaoException.WithStatus` carrying its HTTP status.

## Gotchas

- Only fields (or methods) annotated with `@JsonProperty` are serialized (the mapper turns
  auto-detection off): entities have getters and in-memory state that aren't part of the API.
  JSON property names are camelCase (`clientId`, `stackTrace`), and the page JavaScript reads
  them by name, so rename one only together with `chat.js`.
- Timestamps are Jackson's native ISO-8601 (`JavaTimeModule`), but the front-end's
  `jquery.localtime` parser only accepts exactly three fractional digits or none, and
  `Instant.now()` has microseconds. So every `Instant` in an entity is created with
  `Instant.now().truncatedTo(ChronoUnit.MILLIS)`; keep doing that for any new one.
- Tokens appear in URL paths and route regexes (`AbstractPusaChatController.TOKEN_REGEX`), so
  they may only contain `[a-zA-Z0-9-_]`.
- An entity's HTTP status comes from `PusaChatEntity.getStatus()` (200 unless
  overridden, as `ApiErrorResponse` does); it's never serialized.
- Rendered HTML is minified by curacao-freemarker's `HtmlMinifier`, which *removes* line breaks
  and the whitespace around them (it doesn't turn them into a space). Inline elements that need
  a space between them (a label, text box and button) go on one line with explicit spaces;
  never wrap a sentence across template lines.
- The front-end finds the app's base path in the `<body data-context-path>` attribute set by
  `layout.ftl`; don't hardcode paths in the JavaScript.
- Curacao injects through the component's public constructor; don't add extra public
  constructors to components.
- Curacao does not prefer the most specific `@Mapper`; there is one `ExceptionResponseMapper`
  for `Exception`.
- Curacao caches controller instances in a static, JVM-wide cache, so tests that boot the stack
  need a fresh JVM per test class (surefire `reuseForks=false`).
- `-Dconfig.file` needs a path with a directory in it, or `include "application"` finds nothing.

## Code Style

Same rules as onyx, beacon and jasper, and now literally the same files: Checkstyle, PMD and
the duplicate class/resource check are wired up in the `kolich-parent` POM and read their rules
from `kolich-build-config` on the plugin classpath, so nothing is kept in this repo. All three
run in `mvn package`, and compiler warnings are errors. Notably: `final` params/locals, member
fields end in `_`, non-exception classes are `final`, abstract classes start with `Abstract`,
method/record parameters start on a new line, and the license header on every `.java` file.
