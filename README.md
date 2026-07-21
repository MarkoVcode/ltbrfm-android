# LTBR·FM for Android & Android Auto

A dedicated player for **London Tower Block Radio** (`stream.ltbr.fm/live`)
that works on the phone and — the point of this app — on **Android Auto**.

- **Playback**: ExoPlayer (androidx.media3) plays the Icecast MP3 stream and
  parses ICY metadata natively, so the live *now playing* title shows on the
  car screen, lock screen and notification.
- **`MediaLibraryService`**: one service surfaces the station to Android Auto,
  Bluetooth / steering-wheel controls, the lock screen and Wear OS. The car
  renders its own (mandatory) templated media UI.
- **Phone UI**: a minimal amber-on-black screen in Jetpack Compose — wordmark,
  TX LED (solid red standby, pulsing on air), play/stop key, now-playing line —
  echoing the LTBR·FM desktop receiver.

Sister project: the desktop receiver lives at
[MarkoVcode/ltbrfm-player](https://github.com/MarkoVcode/ltbrfm-player).

## Building

Requirements: JDK 17 and the Android SDK (Android Studio installs both).

```bash
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/
```

or open the project in Android Studio and run it on a device.

CI builds a debug APK on every push (Actions → artifacts) and attaches it to a
draft GitHub Release on `v*` tags.

## Trying it in a car (Android Auto developer mode)

1. Install the APK on your phone (`adb install app-debug.apk`).
2. In the **Android Auto** app: Settings → tap **Version** repeatedly until
   developer mode unlocks → developer settings → enable **Unknown sources**.
3. Connect to the car (or the Desktop Head Unit emulator): LTBR·FM appears in
   the media app launcher.

## Shipping to other people

Android Auto apps must be distributed through **Google Play** and pass review
in the media-apps-for-cars category. That requires a Play developer account
and a release-signed build (add a keystore + `signingConfig`); the debug APK
built by CI is for personal/sideload testing only.

## License

MIT — see [LICENSE](LICENSE).
