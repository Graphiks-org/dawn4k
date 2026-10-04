# Bienvenue sur la Documentation de dawn4k

Ce site regroupe la documentation technique et les références d'API de **dawn4k**, un binding Kotlin Multiplatform vers l'API C de Google Dawn (WebGPU).

---

## 🚀 Fonctionnalités Clés

*   **Dawn desktop, Android, iOS et tvOS** : API C WebGPU brute générée par kextract pour **JVM**, **Android**, **iOS/tvOS (device + simulateur)**, **macOS ARM64** et **Linux x64**.
*   **Natif vérifié** : prébuilds Dawn vérifiés (checksums/manifeste), oracle ABI C et smoke réel instance/adapter/device/buffer.
*   **Runtime** : kffi côté JVM (bibliothèque partagée chargée par un bootstrap généré) et Kotlin/Native (liaison statique).

---

## 🧱 Organisation du Projet

*   **`:dawn4k-native`** — bindings Dawn bas niveau générés (`org.graphiks.dawn4k.raw`), oracle ABI et configuration de liaison.
*   **`:native-smoke`** — consommateur réel créant une instance, un adapter, un device et un buffer mappé.

Voir [Native Dawn binding](native-binding.md) pour la génération, la liaison et le statut du smoke.

---

## 💻 Commandes Utiles

### Exécuter la suite de tests (Fast-Track JVM)
```bash
./gradlew :dawn4k-native:jvmTest
```

### Lancer tous les tests (cibles de l'hôte)
```bash
./gradlew allTests
```

### Vérifier l'oracle ABI C
```bash
./gradlew :dawn4k-native:verifyDawnAbi
```

### Lancer le smoke réel (JVM / macOS ARM64)
```bash
./gradlew :native-smoke:runJvmSmoke
./gradlew :native-smoke:runSmokeMacosArm64
```

### Compiler localement le site MkDocs
```bash
mkdocs build -f docs/mkdocs.yml
```
