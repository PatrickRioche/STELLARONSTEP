# OnStepPilot

Application Android Kotlin / Jetpack Compose pour piloter simplement une monture **OnStep / OnStepX**.

## MVP 0.1

Navigation à cinq écrans : **Home**, **Control**, **Goto**, **Align**, **Config**.

- connexion directe TCP à OnStep ;
- lecture RA/DEC et état `:GU#` ;
- pad N/S/E/W et vitesses de déplacement ;
- tracking ON/OFF, STOP global, HOME, PARK, UNPARK ;
- GOTO RA/DEC et quelques cibles rapides ;
- séquence d'alignement OnStep ;
- persistance de l'adresse IP / port ;
- tests du parseur de statut ;
- CI GitHub Actions.

## Origine

Le design fonctionnel du GOTO/Ciel est prévu pour réutiliser les éléments déjà validés dans **PatrickRioche/STELLARPILOT**, particulièrement `SkyScreen`, `SkyViewModel` et le catalogue de cibles, tout en retirant la dépendance au serveur Raspberry/FastAPI.

## Environnement

- Android Studio récent
- JDK 17
- Android SDK 36
- Kotlin 2.2.0
- AGP 8.12.0
- Gradle 8.13

## Important : wrapper Gradle

Le fichier binaire `gradle-wrapper.jar` doit être présent dans `gradle/wrapper/`. Lors de la création du dépôt, le plus simple est de copier celui du projet `STELLARPILOT/android/gradle/wrapper/gradle-wrapper.jar`, qui utilise déjà Gradle 8.13.

## Première mise en route

1. Ouvrir le dossier dans Android Studio.
2. Vérifier `gradle/wrapper/gradle-wrapper.jar`.
3. Synchroniser Gradle.
4. Dans **Config**, saisir l'IP et le port TCP réellement exposés par OnStep.
5. Tester d'abord `Home -> Actualiser`, puis les commandes sans mouvement dangereux.
6. Vérifier PARK/HOME et les limites de la monture avant les GOTO.

## Git

```bash
git init
git add .
git commit -m "feat: bootstrap OnStepPilot Android app"
git branch -M main
git remote add origin <URL_DU_NOUVEAU_REPO>
git push -u origin main
```

Voir `docs/ARCHITECTURE.md`, `docs/ONSTEP_COMMANDS.md` et `docs/MIGRATION_STELLARPILOT.md`.
