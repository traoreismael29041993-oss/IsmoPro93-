<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/494f1119-cb89-42dc-9a63-c98d41a6e1a8

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

---

## CI : build automatique AAB / APK (GitHub Actions)

Un workflow GitHub Actions a été ajouté dans `.github/workflows/android-build.yml` pour :
- Construire un AAB et un APK (release si tu fournis un keystore, sinon debug)
- Créer automatiquement une Release GitHub contenant les artefacts générés

### Secrets requis (Repository > Settings > Secrets and variables > Actions)

- KEYSTORE_BASE64 (optionnel pour build release) :
  - Valeur : le contenu du fichier keystore (.jks) encodé en base64 sur une seule ligne.
  - Commande Linux/macOS pour générer la valeur :
    - base64 my-upload-key.jks | tr -d '\n'
  - Exemple (générer un keystore localement si tu n'en as pas) :
    - keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000

- STORE_PASSWORD (requis si KEYSTORE_BASE64 fourni) : mot de passe du keystore
- KEY_PASSWORD (requis si KEYSTORE_BASE64 fourni) : mot de passe de la clé (alias)

Note : Le workflow utilise également le secret `GITHUB_TOKEN` fourni automatiquement par GitHub pour créer la Release — tu n'as rien à configurer pour celui-ci.

### Comportement du workflow

- Si KEYSTORE_BASE64 est défini :
  - Le keystore est recréé dans la racine du workspace (my-upload-key.jks) et le build release signé est produit.
  - Artefacts uploadés sur la Release : `app-release.aab` et `app-release.apk` (si produits).

- Si KEYSTORE_BASE64 n'est pas défini :
  - Le workflow compile un build debug et publie `app-debug.aab` et `app-debug.apk` dans la Release.

### Déclenchement

- Automatique : push sur la branche `main`.
- Manuel : via l'onglet Actions → choisir `Android CI - build AAB/APK` → Run workflow.

### Récupérer les artefacts

1. Ouvre l'onglet Releases du dépôt (https://github.com/<owner>/<repo>/releases)
2. Choisis la Release créée par le workflow (tag `build-<run_number>`)
3. Télécharge les fichiers attachés (`*.aab`, `*.apk`)

### Exemples de commandes locales (optionnel)

- Build debug localement :
  - chmod +x ./gradlew && ./gradlew bundleDebug assembleDebug

- Build release localement (en supposant un keystore local `my-upload-key.jks`) :
  - chmod +x ./gradlew && ./gradlew bundleRelease assembleRelease -Pandroid.injected.signing.store.file="${PWD}/my-upload-key.jks" -Pandroid.injected.signing.store.password=STORE_PASSWORD -Pandroid.injected.signing.key.alias=upload -Pandroid.injected.signing.key.password=KEY_PASSWORD

---

Si tu veux, je peux aussi :
- Générer et commiter un keystore de test (non recommandé pour production),
- Ajouter des étapes pour publier automatiquement sur Google Play (via fastlane) — il faudra des droits supplémentaires et secrets Play Store.

