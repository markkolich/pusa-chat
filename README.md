# Pusa Chat

A lightweight, all in memory, multi-user web based chat application.

*Pusa* is Tagalog for "cat".

Create a room, share its name, and chat. There are no accounts, no database and nothing to
install for the people chatting: rooms, messages and the list of who's in them live only in the
server's memory, so **everything is gone on restart.**

Pusa Chat is built for Java 21 and runs from a single (fat/uber) JAR: an embedded Jetty 11 server
running [Curacao](https://github.com/markkolich/curacao), with pages rendered by FreeMarker and a
small jQuery front end that long polls the server for new events.

## Features

* **Rooms by name.** Join a room by typing its name (its "room key"), create one, or have the
  server generate a random name. Each room opens in its own small popup window.
* **Shareable links.** Creating a room shows a link (`/pusa-chat/?key=<room>`) that opens the
  homepage with the room name already filled in.
* **Live updates** without WebSockets: messages, deletes, who's typing, people joining and
  leaving, and the user count arrive over long polling, which works through any proxy that
  passes plain HTTP.
* **Recent history.** Each room keeps its most recent messages (50 by default), so people who
  join late see what they missed.
* **Safe messages.** Messages are HTML escaped on the server, and `http://` and `https://` URLs
  in them become links that open in a new tab.
* **Deleting messages.** A deleted message disappears from every window in the room.
* **Typing and activity indicators.** The room shows who's typing, and counts a person as
  inactive after 10 minutes without typing in or focusing the chat window.
* **Idle cleanup.** A client that stops polling (the window closed, the laptop went to sleep) is
  removed from its room, after 30 to 60 seconds by default.
* **Unforgeable room links.** The room page's URL carries a signed room token rather than the
  room's internal id, so it can't be forged or guessed.

## Requirements

* Java 21 or later
* [Apache Maven](https://maven.apache.org) 3, to build

The front end build needs Node.js, but you don't have to install it: Maven downloads its own copy
into the project (see [JavaScript and CSS](#javascript-and-css)).

## Build

Clone the repository and build it with Maven:

```bash
git clone https://github.com/markkolich/pusa-chat.git
cd pusa-chat
mvn package
```

`mvn package` builds the JavaScript and CSS bundles, compiles the code, runs the tests, checks the
code with Checkstyle and PMD, and puts the JARs in `dist/`:

```
dist/pusa-chat-1.0-runnable.jar   # everything needed to run, including Jetty
dist/pusa-chat-1.0.jar            # just the application classes and web files
```

## Run

```bash
java -jar dist/pusa-chat-1.0-runnable.jar
```

Then open http://127.0.0.1:8080/pusa-chat/.

| Option | Default | Description |
|---|---|---|
| `--host` | `127.0.0.1` | The address to listen on. Use `0.0.0.0` to listen on every interface. |
| `--port` | `8080` | The HTTP port. |

For example:

```bash
java -jar dist/pusa-chat-1.0-runnable.jar --host 0.0.0.0 --port 9090
```

Stop the server with Ctrl-C. Every room and message goes with it.

## Configuration

Pusa Chat is configured with HOCON using [lightbend/config](https://github.com/lightbend/config).
The defaults are in [`src/main/resources/application.conf`](src/main/resources/application.conf),
which is built into the JAR.

To change a setting, create a file that includes the defaults and overrides what you need, for
example `~/pusachat-dev.conf`:

```hocon
include "application"

pusa-chat {
  dev-mode = true
}
```

and pass it to the server with `-Dconfig.file`:

```bash
java -Dconfig.file=$HOME/pusachat-dev.conf -jar dist/pusa-chat-1.0-runnable.jar
```

> **Note:** give `-Dconfig.file` a path that has a directory in it (`$HOME/pusachat-dev.conf` or
> `./pusachat-dev.conf`). With a bare file name (`-Dconfig.file=pusachat-dev.conf`), the
> `include "application"` line silently finds nothing, and the server fails at startup because
> every other setting is missing.

### Settings

| Setting | Default | Description |
|---|---|---|
| `pusa-chat.context-path` | `"/pusa-chat"` | The path the app is served under. |
| `pusa-chat.dev-mode` | `false` | When `true`, error pages show the error message, and JSON errors include a `stackTrace`. Leave it off in production. |
| `pusa-chat.remove-inactive-users-after` | `30s` | How long a client can go without polling for events before it's removed from its room. The cleanup also runs at this interval. |
| `pusa-chat.max-cached-messages-per-room` | `50` | How many recent messages each room keeps and shows to people who join. |
| `pusa-chat.long-poll-timeout` | `20s` | How long a request for the next event waits before returning a `NOOP`, after which the browser asks again. |
| `curacao.async-context-timeout` | `1m` | The longest any request may take. Must stay above `long-poll-timeout`. |
| `curacao.thread-pool.size` | `256` | Request threads. Every open chat window holds one while it waits for its next event, so this caps the number of chat windows open at once across all rooms. |

Durations accept HOCON's units, e.g. `500ms`, `30s`, `5m`.

## Running behind nginx

Pusa Chat speaks plain HTTP and is meant to run behind a reverse proxy such as nginx, which
handles TLS and security headers. Serve it under the same path as `pusa-chat.context-path` so no
URL rewriting is needed:

```nginx
location /pusa-chat/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

nginx's default `proxy_read_timeout` of 60 seconds is comfortably longer than the 20 second long
poll. If you raise `pusa-chat.long-poll-timeout`, keep it below `proxy_read_timeout`, Jetty's 60
second idle timeout and `curacao.async-context-timeout`, or waiting requests get cut off and the
browser shows connection errors.

To serve the app under a different path, change `pusa-chat.context-path` and the nginx `location`
together.

## How it works

1. **Opening a room.** The homepage asks the server to join, create or randomly name a room. The
   server answers with the room's *room token*: the room's internal id plus an HMAC-SHA256
   signature. The browser opens the room page at `/pusa-chat/api/room/<room token>`.
2. **Registering.** The room page registers with the room token and gets back a *client token*
   (the room id and a new client id, again signed). Every later request uses the client token.
3. **Long polling.** The browser asks for the next event (`GET .../chat/event/<client token>.json`).
   The server holds the request until something happens in the room, or until
   `long-poll-timeout` passes and it returns a `NOOP`. Either way, the browser asks again 100 ms
   later; after an error it waits 5 seconds. Each poll also tells the server the client is still
   there.
4. **Sending.** Messages, deletes, typing and activity changes are short POST and DELETE
   requests. The server turns each into an event and queues it for every client in the room,
   including the sender.
5. **Cleaning up.** A background task regularly removes clients that haven't polled within
   `remove-inactive-users-after` and tells the rest of the room (a `CLIENT_LEFT` for each, then
   the new count). The room page shows joins and leaves as a grey line among the messages
   ("Someone joined the room."); they aren't kept in the room's history. A client is only
   noticed leaving once it stops polling, so "left" shows up a little after someone closes the
   page: between one and two `remove-inactive-users-after` periods (30 to 60 seconds, by
   default). If a removed client comes back, its next request gets a `412` and the browser
   registers again.

Tokens are signed with a random key generated each time the server starts, so room links stop
working after a restart, along with the rooms themselves. To share a room, share its name or the
`?key=` link from the homepage; those keep working, since joining by name offers to recreate a
room that no longer exists.

## HTTP API

The browser uses a small JSON API, which also works from `curl` or any other HTTP client. All
paths are under the context path (`/pusa-chat` by default). Requests send their parameters as
form fields (`application/x-www-form-urlencoded`), including on `DELETE`. Timestamps are ISO-8601 in UTC
with millisecond precision, e.g. `2026-10-12T02:42:20.443Z`; when the milliseconds are exactly
zero they're left off (`2026-10-12T02:42:20Z`).

### Pages

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | The homepage. `?key=<room>` fills in the room name. |
| `GET` | `/api/room/{roomToken}` | The chat page for a room. |

### Rooms

| Method | Path | Parameters | Description |
|---|---|---|---|
| `POST` | `/api/room/join.json` | `name` | Returns an existing room. `404` if it doesn't exist. |
| `POST` | `/api/room/create.json` | `name` | Returns the room with that name, creating it if needed. |
| `GET` | `/api/room/random.json` | | Creates a room with a random 10 character name. |

Room names are 1 to 36 letters, digits, `-` or `_`, and are lowercased, so `Demo` and `demo` are
the same room.

```bash
curl -d name=demo http://127.0.0.1:8080/pusa-chat/api/room/create.json
```

```json
{
  "token": "77ceafd4-4e9c-4278-a87f-df953628631f_ad99119acc35f6ee168be390d39c4b8f6f274e834a9ecd9f522d6fc32bc2ed8b",
  "createdOn": "2026-10-12T02:42:20.305Z",
  "name": "demo"
}
```

### Chat

`{roomToken}` is the `token` of a room; `{clientToken}` is the `token` returned by `register`.

| Method | Path | Parameters | Returns |
|---|---|---|---|
| `POST` | `/api/chat/register/{roomToken}.json` | | A new client session in the room. |
| `GET` | `/api/chat/event/{clientToken}.json` | | The client's next event, waiting up to `long-poll-timeout`. |
| `GET` | `/api/chat/log/{clientToken}.json` | | The room's recent messages, oldest first. |
| `POST` | `/api/chat/message/{clientToken}.json` | `message` | The `MESSAGE` event that was posted. |
| `DELETE` | `/api/chat/message/{clientToken}.json` | `messageId` | The `DELETE` event that was posted. |
| `POST` | `/api/chat/typing/{clientToken}.json` | `typing` (`true`/`false`) | The room's `TYPING` event. |
| `POST` | `/api/chat/inactivity/{clientToken}.json` | `active` (`true`/`false`) | The room's `CLIENT_STATUS` event. |

A session from `register`:

```json
{
  "roomId": "77ceafd4-4e9c-4278-a87f-df953628631f",
  "clientId": "4388c08d-9555-4391-969f-d8eb6adc9406",
  "token": "77ceafd4-4e9c-4278-a87f-df953628631f_4388c08d-9555-4391-969f-d8eb6adc9406_0abf05462fc996baac6878acf67489ad58bac784a83c54448fe22ec2f2cd0489",
  "lastAccessed": "2026-10-12T02:42:20.387Z"
}
```

A chat log:

```json
{
  "log": [
    {
      "id": "d66d1a45-dc85-42a0-8924-ec6d4ea7ef9f",
      "when": "2026-10-12T02:42:20.443Z",
      "type": "MESSAGE",
      "from": "4388c08d-9555-4391-969f-d8eb6adc9406",
      "html": "hi &lt;b&gt;there&lt;/b&gt; see <a href=\"https://example.com/?q=1\" target=\"_blank\">https://example.com/?q=1</a>"
    }
  ]
}
```

### Events

Every event has an `id`, a `when` timestamp and a `type`, plus fields that depend on the type:

| Type | Extra fields | Sent when |
|---|---|---|
| `MESSAGE` | `from` (client id), `html` (the escaped, linked message) | Someone posts a message. |
| `DELETE` | `delete` (the deleted message's id) | Someone deletes a message. |
| `TYPING` | `typing` (ids of the clients typing) | Someone starts or stops typing. |
| `CLIENT_JOINED` | `clientId` | Someone joins the room. |
| `CLIENT_LEFT` | `clientId` | Someone leaves the room: they stopped polling and were removed (see below). |
| `CLIENT_STATUS` | `active` (clients in the room), `inactive` (how many of them are inactive) | Someone joins, leaves, or goes inactive or active again. |
| `NOOP` | | Nothing happened within `long-poll-timeout`. |

Despite its name, `active` counts every client in the room, inactive ones included; the room
page shows it as e.g. "2 users, 1 inactive".

### Errors

Errors come back as JSON with an HTTP status:

```json
{"message": "Room not found: nope"}
```

| Status | Meaning |
|---|---|
| `400` | A missing or invalid parameter, an invalid room name, or a malformed or forged token. |
| `404` | The room doesn't exist (e.g. after a restart), or there's no such path or method. |
| `412` | The client was removed from the room for being idle. Register again. |
| `500` | Something went wrong on the server; the details are logged. |

In dev mode, errors also include a `stackTrace`. Paths that don't end in `.json` get an HTML
error page instead.

## Development

### IntelliJ IDEA

The project includes a shared run configuration, **pusachat-dev (localhost:8080)**, in `.run/`.
It runs `pusachat.Application` from the project root with
`-Dconfig.file=$USER_HOME$/pusachat-dev.conf`, so create that file first (see
[Configuration](#configuration)). Run that way, templates and static files are served straight
from `src/main/webapp`: edit a template and reload the page, no rebuild needed. JavaScript and CSS
changes need gulp to rebuild the bundles first (see below).

### JavaScript and CSS

The front end is built with [gulp](https://gulpjs.com), run by Maven through the
[frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin).
Everything lives in `src/main/webapp/static`:

```
css/pusachat.css            the stylesheet
js/pusachat/                the app's own JavaScript (linted with ESLint)
vendor/                     third party libraries: jQuery 1.7.1 and its plugins, json2
img/                        images, served as they are
package.json                npm dependencies (gulp, ESLint, terser, clean-css)
gulpfile.js                 the build: which files are bundled, and in what order
eslint.config.js            the ESLint rules
```

During `mvn package`, Maven installs Node.js and npm into `src/main/webapp/static/node`, runs
`npm ci`, then runs `gulp release`, which:

1. lints `js/pusachat/**/*.js` with ESLint, failing the build on any error;
2. concatenates the libraries and app scripts, in the order listed in `gulpfile.js`, into
   `build/app.js`, and the CSS into `build/app.css`;
3. minifies them into `release/app.min.js` (terser) and `release/app.min.css` (clean-css);
4. puts a copyright banner at the top of both minified files.

Pages load `release/app.min.{js,css}`, with the build's git commit in the URL (`?v=<commit>`) so
browsers fetch fresh copies after a deploy, and only those files go into the JAR. In dev mode
(`pusa-chat.dev-mode = true`), pages load the unminified `build/app.{js,css}` instead, with a
timestamp in the URL so they're never cached.

To add a script or stylesheet, put it in `js/pusachat/` or `vendor/` and add it to the list in
`gulpfile.js`; the order matters, since later scripts use what earlier ones define.

While working on the front end, have gulp rebuild `build/` whenever a file changes, and reload
the page (in dev mode) to see the change:

```bash
cd src/main/webapp/static
node/node node_modules/gulp/bin/gulp.js dev
```

Run `mvn package` (or at least `mvn initialize`) once first, so Node.js and the npm packages are
installed. The other gulp tasks are `release` (the full build), `eslint-js`, `concat-js`,
`concat-css`, `minify-js`, `minify-css` and `clean`. The generated `node/`, `node_modules/`,
`build/` and `release/` directories are ignored by git, and `mvn clean` removes `build/` and
`release/`.

### Tests

```bash
mvn test
```

The end-to-end tests in `PusaChatEndToEndTest` boot the whole server on a random port and drive
it over HTTP the way the browser does: creating and joining rooms, two clients chatting, deletes,
typing, inactivity, idle clients being removed, long polls that time out, and rejected tokens and
parameters. They use `src/test/resources/pusachat-test.conf`, which shortens the timeouts so the
tests run in seconds. Unit tests cover the token signer and the message to HTML conversion.

### Code style

Checkstyle (`build/checkstyle/config.xml`) and PMD (`build/pmd/ruleset.xml`) run as part of
`mvn package`, and compiler warnings are errors. Notably:

* `final` parameters and local variables
* member fields end in `_`
* classes that aren't exceptions are `final`; abstract classes are named `Abstract*`
* method parameters start on a new line
* every `.java` file starts with the license header in `build/checkstyle/LICENSE.txt`

### Project layout

```
build/                      Checkstyle, PMD and versions-maven-plugin rules
src/main/java/pusachat/
  Application.java          starts Jetty and Curacao; the command line options
  components/chat/          rooms, sessions, events and the token signer
  components/config/        typed access to application.conf
  components/freemarker/    FreeMarker setup and page rendering
  controllers/              the homepage, room and chat endpoints
  entities/                 rooms, sessions, chat logs and events (serialized with Jackson)
  exceptions/               errors and the HTTP status each one returns
  mappers/response/         turns pages and errors into HTTP responses
  util/                     the JSON ObjectMapper and message to HTML conversion
src/main/resources/         application.conf, logback.xml
src/main/webapp/
  templates/                FreeMarker pages (layout, homepage, room, error)
  static/                   CSS, JavaScript, images and the gulp build (see JavaScript and CSS)
src/test/                   end-to-end and unit tests
```

## Limitations

* **Nothing is persisted.** Rooms, messages and sessions live in memory and are lost on restart.
* **Rooms are never deleted.** An empty room stays in memory until the server restarts, so memory
  use grows with the number of rooms created.
* **One server only.** State lives in a single JVM, so Pusa Chat can't be load balanced across
  several servers.
* **Open windows are capped by threads.** Each open chat window holds a request thread while it
  long polls, so `curacao.thread-pool.size` (256) limits how many can be open at once; past that,
  requests queue.
* **Anyone who knows a room's name can join it.** Room names aren't secret: use an obscure or
  random name for anything private.
* **Old front end.** The browser code uses jQuery 1.7 and plugins of the same era.

## License

Copyright (c) 2026 Mark Kolich

Pusa Chat is released under the MIT License; see [LICENSE](LICENSE).
