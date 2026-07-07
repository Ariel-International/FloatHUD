# FloatHUD

One shared Android codebase, four separately publishable apps. Each app is a draggable,
always-on-top overlay (`TYPE_APPLICATION_OVERLAY`) built around a single `OverlayService`, and
differs only in which mode is free:

| App (Play Store title)                     | `applicationId`               | Free mode    |
|----------------------------------------------|--------------------------------|--------------|
| Stopwatch Overlay – FloatHUD                  | `it.top.floathudstopwatch`    | Stopwatch    |
| Countdown Timer Overlay – FloatHUD            | `it.top.floathudcountdown`    | Countdown    |
| World Clock Overlay – FloatHUD                | `it.top.floathudworldclock`   | World Clock  |
| Floating Clock – FloatHUD                     | `it.top.floathudclock`        | Clock        |

In every app the other three modes, and the ability to show more than one overlay at once, are
gated behind a single one-time "Pro" purchase (Google Play Billing, non-consumable, product id
`pro_unlock`).

## Modes

- **Stopwatch** — mm:ss, start/stop/reset.
- **Countdown** — set a duration in minutes; vibrates and plays the default alarm sound at zero,
  with a Dismiss button.
- **World Clock** — one or more time zones, ticking live. Uses `java.time` (`ZonedDateTime`,
  `ZoneId`) so DST is automatic — no manual UTC offset math. Zones are picked from
  `ZoneId.getAvailableZoneIds()`.
- **Clock** — a single always-on local-time display.

## Architecture

- `OverlayService` holds every currently-shown overlay window (one per drag-to-move
  `WindowManager` view) so free users are capped at one window and Pro users can stack several.
  It drives one shared 250ms ticker across all of them.
- `OverlayModeController` is the small interface each mode implements
  (`StopwatchController`, `CountdownController`, `WorldClockController`, `ClockController`).
- `OverlayPermission` wraps the `SYSTEM_ALERT_WINDOW` ("Display over other apps") flow —
  `Settings.canDrawOverlays` checked in `MainActivity.onResume()`, `ACTION_MANAGE_OVERLAY_PERMISSION`
  to request it.
- `Prefs` persists the cached entitlement, last-used mode, countdown duration, and saved world
  clock zones.
- `PurchaseManager` wraps `BillingClient` for the one-time Pro purchase.
- Which mode is free per app is a single Gradle product flavor setting
  (`buildConfigField "String", "FREE_MODE", ...` in `app/build.gradle`) — `OverlayMode.requiresPro`
  reads `BuildConfig.FREE_MODE`, so no per-app code branching is needed anywhere else.

## Before you ship this

1. **Create the `pro_unlock` in-app product** in each app's Play Console listing — until that
   exists, `PurchaseManager.purchase()` has nothing to launch.
2. **Add real launcher icons.** Every flavor currently falls back to
   `@android:drawable/sym_def_app_icon` (a system placeholder) so the build isn't blocked;
   generate real per-flavor icons with Android Studio's Image Asset tool and drop them into
   `app/src/<flavor>/res/mipmap-*`.
3. **Test on a real device**, not just an emulator — drag behavior, the overlay permission flow,
   and the alarm/vibration on countdown all need to be exercised by hand.
4. Each flavor needs its own Play Console app listing and signing config for release builds.

## Building via GitHub Actions (no local Android Studio needed)

1. Push this repo to GitHub.
2. Go to the **Actions** tab — the `Build APK` workflow runs on push to `main`, or trigger it
   manually via "Run workflow". `gradle assembleDebug` is an aggregate task, so it builds all
   four flavors' debug APKs in one run; to build just one locally use e.g.
   `gradle assembleCountdownDebug`.
3. Download the `floathud-debug-apks` artifact from the finished run — it contains all four
   APKs (`app-stopwatch-debug.apk`, `app-countdown-debug.apk`, `app-worldclock-debug.apk`,
   `app-clock-debug.apk`).

## Known rough edges

- No launcher icons or Play Store listing assets yet (see above).
- A signed release build needs a keystore, which isn't set up here — CI only builds unsigned
  debug APKs for now.
