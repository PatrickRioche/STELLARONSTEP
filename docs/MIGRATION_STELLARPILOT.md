# Réutilisation de StellarPilot

Le projet reprend l'approche UI de l'écran **Ciel & Cible** de StellarPilot sans dépendre de son serveur FastAPI.

À migrer dans un second temps :

1. modèles de catalogue (`SkyStar`, objets du système solaire, Messier/NGC/IC) ;
2. présentation par cartes et filtres de `SkyScreen` ;
3. logique de sélection de cible de `TargetCatalogPanel` ;
4. conversion/validation RA/DEC déjà testée dans StellarPilot ;
5. favoris et historique de GOTO.

À ne pas reprendre tel quel :

- appels `StellarPilotApiClient` ;
- dépendances à l'astrométrie/caméra/serveur Raspberry ;
- fonctions propres à la capture.
