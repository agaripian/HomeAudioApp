# Home Audio Gem

**Home Audio Gem** is an Android app for controlling a multi-zone home audio system from your phone or tablet. It talks to a backend API on your local network so you can manage power, volume, tone, and source for each zone or for all zones at once.

---

## What It Does

- **Zone list** — See all audio zones (rooms) and their status at a glance.
- **Per-zone control** — For each zone you can:
  - Turn power on/off
  - Adjust volume, treble, and bass
  - Choose input source (e.g. Source 1–6)
  - Rename the zone
- **Control all zones** — One screen to set power, mute, volume, treble, bass, and source for every zone.
- **All zones off** — Single action to turn off all zones.
- **Settings** — Configure the API server URL, number of amplifiers, and app theme (light / dark / system).
- **Auto-refresh** — Zone list refreshes every 10 seconds so the UI stays in sync with the system.

The app is designed to work with a compatible backend API (e.g. a small server that controls your amps). You set the server base URL in Settings; the app does not include the hardware control logic.

---

## Requirements

- **Android:** min SDK 24, target SDK 35 (Android 7.0+).
- **Network:** Device and backend must be on the same LAN (or reachable); the app uses HTTP (and optionally cleartext) to talk to your API.
- **Backend:** A running API that implements the endpoints the app expects (zones, attributes, amp count, etc.). Server URL is configurable in the app (default: `http://192.168.1.23:3000/api/`).

---

## Tech Stack

- **Language:** Kotlin  
- **UI:** Android Views (ViewBinding), Material Design, ConstraintLayout, RecyclerView; some Jetpack Compose (e.g. theme).  
- **Networking:** Retrofit 2, OkHttp, Gson.  
- **Async:** Kotlin Coroutines.  
- **Min SDK:** 24 | **Compile/Target SDK:** 35  

---

## Project Structure (high level)

- `app/src/main/java/com/example/homeaudiogem/`
  - **MainActivity** — Zone list, “Control All”, “All Off”, toolbar, periodic refresh.
  - **activities/** — ControlAllActivity (master controls), SettingsActivity (server URL, amp count, theme).
  - **adapters/** — ZoneAdapter for the zone list and per-zone controls.
  - **api/** — ApiService (Retrofit interface), ApiClient (Retrofit/OkHttp setup), ZoneRepository.
  - **models/** — Zone data class (power, volume, treble, bass, source, name, etc.).
- **Resources** — Layouts, strings, themes under `app/src/main/res/`.

---

## Setup

1. **Clone or open the project** in Android Studio (or your IDE).
2. **Backend:** Run your home-audio API server and note its base URL (e.g. `http://<your-server-ip>:3000/api/`).
3. **Build & run** the app on a device or emulator that can reach that server.
4. **First run:** Open **Settings**, set the **Server URL** to your API base URL, then save. Return to the main screen; zones should load if the API is up and returning data.

---

## Configuration (in-app)

- **Server URL** — Base URL of your API (e.g. `http://192.168.1.23:3000/api/`). Stored in app preferences.
- **Number of amplifiers** — Sent to the backend via the API; configurable in Settings.
- **Theme** — Light, Dark, or System default.

---

## API Contract (what the app expects)

The app assumes a REST-style API under the configured base URL, for example:

- `GET  /zones` — List of zones (JSON).
- `GET  /zones/{zone}` — Single zone.
- `POST /zones/{zone}/{attribute}` — Set one attribute (body: plain text value).
- `POST /allzones/{attribute}` — Set one attribute for all zones.
- `GET  /ampCount` — Amp count (e.g. `{"count": 1}`).
- `POST /ampCount` — Set amp count (body: plain text).
- `POST /sortOrder` — Save zone order (JSON body).

Zone attributes used by the app include things like power (`pr`), volume (`vo`), treble (`tr`), bass (`bs`), channel/source (`ch`), name (`nm`), mute (`mu`), etc. Your backend must implement these endpoints and map them to your hardware.

---

## License

This project is provided as-is. Use and modify it according to your needs. If you distribute it or build on it, check and comply with the licenses of all dependencies (Android, Retrofit, OkHttp, Gson, Kotlin, etc.).
