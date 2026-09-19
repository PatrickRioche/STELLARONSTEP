# Changelog StellarOnStep

Ce fichier conserve les fonctions ajoutees, les comportements modifies et les bugs corriges.
Les tags de release restent crees uniquement apres validation materielle sur la monture.

## V0.1.2 - 2026-09-19

### Connexion et session OnStepX
- Session TCP OnStepX partagee entre les ecrans de l'application.
- Polling d'etat rendu sequentiel pour eviter les lectures TCP concurrentes.
- Reduction des faux etats connexion/deconnexion lors d'une lecture ponctuellement manquee.

### GOTO
- Catalogue et ecran GOTO prepares pour V0.1.2.
- Correction de l'ordre d'activation du suivi avant GOTO.
- StellarOnStep n'impose plus `:Te#` avant `:MS#`.
- OnStepX decide lui-meme si le GOTO est autorise.
- Le suivi est tente apres acceptation du GOTO.
- Diagnostic GOTO enrichi avec les codes `MS` et `GE`.

### Control
- Clarification des etats PARK / HOME / UNPARK.
- Etat NON PARKEE : HOME et PARK disponibles, UNPARK indisponible.
- Etat PARKEE : UNPARK disponible, HOME et PARK indisponibles.
- Etat PARK EN COURS : commandes HOME/PARK/UNPARK bloquees.
- Ajout de `Rapide 50 %` via `:RS#`.
- Ajout de `MAX 100 %` via `:R9#`.

### Home
- Affichage du type de suivi : OFF, SIDERAL, LUNAIRE, SOLAIRE ou KING.
- Affichage de la version reelle de l'application via `BuildConfig.VERSION_NAME`.
- Clarification de l'etat de parking.

### Initialisation OnStepX depuis Android
- Methode preferee sur telephone avec GPS : position + date + heure + fuseau UTC envoyes a OnStepX.
- Si le GPS est indisponible, StellarOnStep conserve la position OnStepX et synchronise date/heure/fuseau uniquement.
- RESET HOME reste volontairement manuel : la monture doit etre placee physiquement en position HOME avant son utilisation.
- Acces direct a `INITIALISER DEPUIS LE TELEPHONE` depuis Home et Config.

### Tracabilite
- Creation de ce `CHANGELOG.md`.
- Les prochains correctifs V0.1.2 doivent ajouter leur resume ici.
- Au passage sur `main`, le tag annote `V0.1.2` reprendra le resume de ce changelog.

### Commits principaux V0.1.2
- `Share OnStepX session and clarify startup park state`
- `Clarify tracking mode and keep UNPARK label`
- `Fix GOTO tracking activation order`
- `Show app version and add MAX slew control`
- `Fix startup park control states`

### Validation release
- Validation materielle V0.1.2 confirmee sur le telephone A55 et la monture OnStepX.
- Cette version est promue de `dev` vers `main` et taguee `V0.1.2`.
## V0.1.1 - Stable
- Version stable actuellement conservee sur `main`.

## V0.1.0
- Premiere version taguee de StellarOnStep.