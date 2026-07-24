# 📱 Pulse

Pulse is a professional Android media assistant and event coverage application built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**. It empowers users and media teams to manage events, capture and upload assets, and generate customized, high-quality AI promotional content in real-time leveraging state-of-the-art Gemini API models.

---

## 🎨 Main Features

- **Event Lifecycle Management**: Create, view, edit, and track comprehensive details of corporate, social, and professional events.
- **AI-Powered Media Center**: Upload photos, videos, and transcripts, automatically analyzing assets for context validation.
- **State-of-the-Art Generative Content**: Instantly generate 7 distinct promotional formats:
  - Event Headlines
  - Facebook/Instagram Long Posts
  - Short-form Posts (X / Threads)
  - Post Captions
  - Optimized Hashtags
  - Corporate News Summaries
  - Multimodal Video Voice-Over Scripts
- **Real-time Local Persistence**: Offline-first capability with **Room Database** caching and robust repositories.
- **Advanced Global Search**: Seamless search and filter options across all events, generated content, and ideas.
- **Robust Sync Status Tracker**: Visual indicators of cloud uploading, content generation progress, and database synchronization.

---

## 🏗️ Architecture

Pulse adheres to the recommended **Android Architecture Guidelines** and utilizes a modularized, clean **MVVM (Model-View-ViewModel)** structure:

```
[ UI / Presentation Layer ]  <--->  [ ViewModel Layer ]
                                            |
                                            v
                                    [ Repository Layer ]
                                            |
                    +-----------------------+-----------------------+
                    |                                               |
                    v                                               v
        [ Local Data Source (Room DB) ]                 [ Remote API Source (Gemini) ]
```

- **Presentation (UI) Layer**: Written entirely in Jetpack Compose, emphasizing responsive grids, edge-to-edge layouts, and Material 3 design tokens.
- **Domain/ViewModel Layer**: Exposes UI state flows (`StateFlow`), handles background asynchronous operations via Coroutines, and holds business logic.
- **Repository (Data) Layer**: The single source of truth that orchestrates data flow between the local Room DB and network models.

---

## 🛠️ Technology Stack & Requirements

- **Programming Language**: 100% Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Minimum SDK (Android Version)**: Android 8.0 (API Level 26)
- **Target SDK (Android Version)**: Android 15.0 (API Level 35)
- **Database Engine**: Room Persistence Library with KSP
- **Asynchronous Flow**: Kotlin Coroutines & Flow
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`)
- **Network & AI Integration**: Gemini API REST integration

---

## 📂 Project Structure

```
pulse/
├── .github/workflows/      # CI/CD pipelines (GitHub Actions)
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── core/           # Navigation routes and UI Core structure
│   │   │   │   ├── models/         # Room Database entities and POJOs
│   │   │   │   ├── screens/        # Compose screens grouped by feature (home, event, settings, splash)
│   │   │   │   ├── services/       # Room DAOs, repositories, and Gemini network engine
│   │   │   │   ├── ui/             # Theme.kt, Typography, and Palette configurations
│   │   │   │   ├── utils/          # Responsive helper utilities
│   │   │   │   └── widgets/        # Reusable custom UI components and cards
│   │   │   └── res/                # XML Resources, assets, and strings.xml
│   │   └── test/                   # Local JVM and Robolectric unit tests
│   └── build.gradle.kts            # App module-level gradle configuration
├── gradle/
│   └── wrapper/                    # Official Gradle Wrapper files
├── build.gradle.kts                # Project root-level gradle configuration
├── settings.gradle.kts             # Module definitions and plugin management
└── gradle.properties               # Global compile properties
```

---

## 🚀 Build and Run Instructions

### 1. Prerequisites
- **Android Studio** (Koala or newer recommended).
- **JDK 17** installed and configured.

### 2. How to Run Locally
1. Clone the repository to your local machine:
   ```bash
   git clone https://github.com/your-username/pulse.git
   cd pulse
   ```
2. Open Android Studio and select **Open an Existing Project**, choosing the root `pulse` directory.
3. Allow Gradle to sync dependencies automatically.
4. Set up your Gemini API credentials by copying `.env.example` to `.env` or specifying it in your system variables:
   ```bash
   cp .env.example .env
   ```
5. Click **Run** in Android Studio to install the application on your physical device or emulator.

---

## 🧪 Testing

Pulse uses modern, reliable local JVM unit tests to ensure high software quality and guard against regressions:

```bash
./gradlew testDebugUnitTest
```

---

## 🤖 GitHub Actions CI/CD

Pulse comes with a production-grade, pre-configured GitHub Actions workflow located in `.github/workflows/android.yml`.

### Key Workflow Features:
- **Automatic Triggers**: Executes on any push or pull request targeting the `main` branch, and when a GitHub Release is created.
- **Dependency Caching**: Utilizes `gradle/actions/setup-gradle@v4` to cache dependencies, cutting build times significantly.
- **Build Quality Guard**: Builds a **Debug APK**, **Release APK**, and **Release AAB (App Bundle)** simultaneously.
- **Automated Tests**: Automatically runs all local unit tests before compiling production bundles.
- **Artifact Upload**: Uploads build products to GitHub Actions summary page as securely downloadable ZIP artifacts.
- **Auto Release Integration**: Attaches the generated production APK and AAB directly to any newly created GitHub Release.

---

## 🗺️ Future Roadmap

- [ ] **Cloud Storage Integration**: Support live Google Drive and Google Docs exports.
- [ ] **Collaborative Workspaces**: Allow teams to view and collaborate on live event feeds.
- [ ] **Advanced Video Rendering**: Incorporate automated subtitle generation on active video coverages.

---

## 🤝 Contributing

Contributions are welcome! Please follow these simple guidelines:
1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/amazing-feature`.
3. Commit your changes: `git commit -m 'Add some amazing feature'`.
4. Push to the branch: `git push origin feature/amazing-feature`.
5. Open a Pull Request.

---

## 👤 Author

Developed with care for professional media coverage and event marketing teams.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.
