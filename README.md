# QuestWeaver (Starter Scaffold)

This is a minimal Android scaffold for the AI D&D DM project.

## Modules
- `app` — Android app, Compose UI, DI bootstrap.
- `core:domain` — domain models & events.
- `core:data` — Room + SQLCipher database and DAOs.
- `core:rules` — deterministic rules engine.
- `feature:map` — simple grid/tokens Compose Canvas.
- `feature:encounter` — (placeholder) turn engine & screens.
- `feature:character` — (placeholder) character/party UI.
- `ai:ondevice` — ONNX Runtime wrapper (stub).
- `ai:gateway` — Retrofit API for remote AI (stub).
- `sync:firebase` — WorkManager backup (stub).

## Build
1. Open in Android Studio Giraffe+ (AGP 8.5, Kotlin 1.9.x).
2. Gradle sync. If `onnxruntime` or `sqlcipher` increases APK size, keep for now; you can remove from catalogs later.
3. Run `app` on an emulator/device. The demo screen renders a simple tactical map (no gameplay yet).

## Notes
- Firebase/Crashlytics are *not* wired to keep the scaffold buildable without `google-services.json`.
- `IntentClassifier` is a stub and will throw until you load a real `.onnx` model from `assets/models/` — replace its init code accordingly.
- Database is encrypted with a demo passphrase; replace with Android Keystore + Biometric unlock.
