# Architecture

## Principe

L'UI ne connaît jamais directement les chaînes LX200/OnStep.

`Compose screens -> AppViewModel -> OnStepRepository -> OnStepTcpClient -> OnStep`

## Écrans

- **Home** : état global, RA/DEC, suivi, GOTO, park, home.
- **Control** : pad N/S/E/W, vitesse, arrêt global, park/home.
- **Goto** : cibles rapides + coordonnées RA/DEC; futur portage du catalogue de StellarPilot.
- **Align** : séquence OnStep `:A[n]#`, GOTO, centrage, `:A+#`, sauvegarde `:AW#`.
- **Config** : IP, port et timeout; futurs transports Bluetooth/USB.

## Évolution prévue

Le transport est isolé. Une interface `OnStepTransport` pourra remplacer le client TCP lorsque les transports Bluetooth Classic, BLE/NUS ou USB série seront ajoutés.
