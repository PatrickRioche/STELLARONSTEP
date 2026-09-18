# StellarOnStep

**StellarOnStep** est une application Android permettant de piloter simplement une monture astronomique équipée de **OnStep / OnStepX**.

L'application est développée en **Kotlin** avec **Jetpack Compose** et communique directement avec OnStep.

## Interface

StellarOnStep est organisé autour de cinq écrans principaux.

### Home

- connexion OnStep ;
- coordonnées RA / DEC ;
- état du tracking ;
- état HOME ;
- état PARK ;
- actualisation de la monture.

### Control

- déplacement Nord / Sud / Est / Ouest ;
- STOP ;
- vitesses Guide / Center / Move / Slew ;
- tracking ON / OFF ;
- HOME ;
- PARK ;
- UNPARK.

### Goto

- saisie RA / DEC ;
- étoiles ;
- objets du ciel profond ;
- système solaire ;
- catalogue astronomique ;
- visibilité des objets ;
- lancement et arrêt du GOTO.

Cet écran pourra réutiliser les concepts de l'écran **Ciel & Cible** de StellarPilot.

### Align

- choix des étoiles ;
- GOTO vers l'étoile ;
- validation ;
- alignement multi-étoiles ;
- sauvegarde du modèle.

### Config

- adresse IP OnStep ;
- port TCP ;
- test de connexion ;
- sauvegarde locale ;
- informations de version.

## Architecture

    Android
       |
       v
    StellarOnStep UI
       |
       v
    ViewModel
       |
       v
    Repository
       |
       v
    OnStep client
       |
       v
    Wi-Fi / TCP
       |
       v
    OnStep / OnStepX
       |
       v
    Monture

Structure principale :

    app/src/main/java/fr/stellaronstep/app/
    ├── core/onstep/
    ├── data/
    ├── feature/home/
    ├── feature/control/
    ├── feature/goto/
    ├── feature/align/
    ├── feature/config/
    ├── ui/theme/
    ├── AppViewModel.kt
    ├── MainActivity.kt
    └── StellarOnStepApp.kt

## Communication OnStep

StellarOnStep communique directement avec OnStep via le protocole compatible **LX200 / OnStep**.

Transport initial :

    Android -> Wi-Fi -> TCP -> OnStep

L'architecture permettra ensuite d'ajouter Bluetooth et USB série.

## Technologies

- Kotlin 2.2.0
- Jetpack Compose
- Material 3
- Android SDK 36
- minSdk 26
- JDK 17
- Android Gradle Plugin 8.12.0
- Gradle 8.13
- Kotlin Coroutines
- JUnit
- GitHub Actions

## Package Android

    fr.stellaronstep.app

## Version

    0.1.0

## Compilation

Sous Windows :

    .\gradlew.bat assembleDebug

Tests :

    .\gradlew.bat test

## StellarPilot

StellarOnStep est indépendant mais pourra réutiliser certains concepts du projet StellarPilot :

https://github.com/PatrickRioche/STELLARPILOT

Notamment :

- SkyScreen ;
- SkyViewModel ;
- catalogue de cibles ;
- calcul des objets visibles ;
- sélection des étoiles d'alignement.

StellarOnStep fonctionnera directement avec OnStep et ne dépendra pas du serveur Raspberry Pi / FastAPI de StellarPilot.

## Roadmap

### 0.1

- [x] architecture Android
- [x] Home / Control / Goto / Align / Config
- [x] client TCP OnStep
- [x] parser de statut
- [x] stockage IP / port
- [x] tests initiaux
- [x] GitHub Actions

### 0.2

- [ ] validation sur une vraie monture OnStep
- [ ] statut temps réel
- [ ] contrôle manuel complet
- [ ] STOP permanent
- [ ] gestion robuste des erreurs réseau

### 0.3

- [ ] catalogue StellarPilot
- [ ] recherche d'objets
- [ ] visibilité des cibles
- [ ] système solaire
- [ ] GOTO catalogue

### 0.4

- [ ] assistant d'alignement avancé
- [ ] sélection automatique des étoiles
- [ ] modèle d'alignement OnStep

### Futur

- [ ] Bluetooth
- [ ] USB
- [ ] mode nuit
- [ ] favoris
- [ ] historique
- [ ] carte du ciel
- [ ] publication Google Play

## Dépôt

https://github.com/PatrickRioche/STELLARONSTEP

**StellarOnStep — Android control for OnStep telescope mounts.**