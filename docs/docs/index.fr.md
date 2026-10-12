# Bienvenue sur la Documentation de dawn4k

Ce site regroupe la documentation technique et les références d'API de **dawn4k**, un binding Kotlin Multiplatform vers l'API C de Google Dawn (WebGPU).

---

## 🚀 Fonctionnalités Clés

*   **Dawn desktop, Android, iOS et tvOS** : API C WebGPU brute générée par kextract pour **JVM**, **Android**, **iOS/tvOS (device + simulateur)**, **macOS ARM64** et **Linux x64**.
*   **Natif vérifié** : prébuilds Dawn vérifiés (checksums/manifeste) et oracle ABI C.
*   **Runtime** : kffi côté JVM (bibliothèque partagée chargée par un bootstrap généré) et Kotlin/Native (liaison statique).

---

## 🧱 Organisation du Projet

*   **`:dawn4k-native`** — bindings Dawn bas niveau générés (`org.graphiks.dawn4k.native`), oracle ABI et configuration de liaison.

Voir [Native Dawn binding](native-binding.md) pour la génération et la liaison.

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

### Compiler localement le site MkDocs
```bash
mkdocs build -f docs/mkdocs.yml
```
