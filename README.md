This is a Kotlin Multiplatform project targeting Android, Desktop (JVM), iOS, Web, Server, built with the [Kotlin Toolchain](https://kotlin-toolchain.org/dev/).

- [/app/androidApp](./app/androidApp) contains the Android application.
- [/app/desktopApp](./app/desktopApp) contains the desktop (JVM) application.
- [/app/iosApp](./app/iosApp) contains the iOS application.
- [/app/shared](./app/shared) holds the code shared across your applications — Compose UI, business logic, and platform-specific implementations. [src](./app/shared/src) is for common code; the sibling `src@<platform>` folders (for example `src@android`) hold code compiled only for the platform named in the folder.
- [/app/webApp](./app/webApp) contains the web application, compiled to WebAssembly with Kotlin/Wasm.
- [/core](./core) holds the code shared across the project, including the greeting helper the apps and the server both use.
- [/server](./server) contains the Ktor server application.

The `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) scripts in the project root are self-bootstrapping wrappers for the Kotlin Toolchain: they download the pinned toolchain version on first use, so no separate installation is required. Build the whole project with `./kotlin build`.

### Windows verification loop

Run the same two checks that GitHub runs on each pull request:

```powershell
.\kotlin.bat test -m shared
.\kotlin.bat build -m androidApp
```

For a faster test run while editing shared Kotlin code, use `.\kotlin.bat test -m shared -p jvm`.

To check the app on Android, start an emulator from Android Studio's Device Manager and wait for its home screen. List connected devices, then launch KeepIt on the chosen device:

```powershell
adb devices -l
.\kotlin.bat run -m androidApp -d emulator-5554
adb -s emulator-5554 logcat -d -s AndroidRuntime
```

Substitute the serial shown by `adb devices -l` if it differs. If `adb` is not on your PATH, use `platform-tools\adb.exe` inside the Android SDK. Check that KeepIt opens and that tapping the demo button displays the greeting. Capture the screen from the emulator toolbar when reviewing UI changes.

The shared Android tests run on the host. The emulator check verifies installation and launch separately.


### Running

Use the run configurations provided by the run widget in your IDE's toolbar. You can also run each module from the command line:

- Android app: `./kotlin run -m androidApp`
- Desktop app: `./kotlin run -m desktopApp`
- iOS app: `./kotlin run -m iosApp`
- Web app: `./kotlin run -m webApp`
- Server: `./kotlin run -m server`

### Testing

Run the project's tests with `./kotlin test`, or `./kotlin test -m <module>` for a single module.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Kotlin Toolchain](https://kotlin-toolchain.org/dev/), [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/), [Kotlin/Wasm](https://kotl.in/wasm/), [Ktor](https://ktor.io/).
