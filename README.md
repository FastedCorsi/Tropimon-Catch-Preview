# Tropimon Catch Preview

By FastedCorsi

Affiche une petite fiche du dernier Pokémon capturé, sans interrompre le jeu.

## Fonctionnalités

- Portrait, nom, niveau, sexe, nature, talent et IV du Pokémon.
- Taille, statut Alpha ou shiny et insignes avec leur description au survol.
- Seuls les chiffres des IV sont colorés : rouge de 0 à 10, blanc de 11 à 20, vert de 21 à 30 et jaune pour 31.
- Talent caché affiché en jaune ; une espèce avec un seul talent reste en couleur normale.
- Fenêtre déplaçable : elle conserve sa dernière position pendant la session de jeu.
- Fermeture automatique après 20 secondes, ou manuelle avec `×` ou `Suppr`.
- À chaque nouvelle capture, la fiche affiche le nouveau Pokémon, en combat comme hors combat.
- Les échanges et transferts officiels entre le PC et l'équipe ne déclenchent pas de fiche.
- Relâchement près d'un PC avec confirmation, sans délai pour confirmer. Le Pokémon choisi reste le même si une autre capture arrive entre-temps. Le dernier Pokémon de l'équipe est protégé.
- Interface adaptée à la langue du jeu, avec traductions françaises et anglaises.

## Installation

Mod **client** pour Minecraft **1.21.1**, avec Fabric Loader **0.17.2 ou supérieur**, Fabric API **0.116.6+1.21.1 ou supérieur** et Cobblemon **1.8.0 ou supérieur**.

Ajouter le JAR compilé à l'instance Fabric utilisée pour jouer. Aucun autre mod Tropimon n'est nécessaire. Ne pas le charger en même temps qu'une version de Compagnion intégrant Catch Preview.

## Compiler les sources

Installer **Java 21** et fournir un JAR Cobblemon **Fabric pour Minecraft 1.21.1**.

Windows :

```powershell
.\gradlew.bat build "-PcobblemonJar=chemin/vers/Cobblemon-fabric.jar"
```

Linux / macOS :

```sh
./gradlew build -PcobblemonJar=chemin/vers/Cobblemon-fabric.jar
```

Le premier build télécharge Gradle et les dépendances officielles. Il lance aussi les tests.

Le JAR du mod est généré dans `build/libs/`. Utiliser le fichier sans le suffixe `-sources`.

## Organisation du code

- `src/main/java/fr/tropimon/catchpreview` : point d'entrée du mod, puis cinq packages :
  - `capture` : historique des Pokémon connus et détection des transferts.
  - `preview` : gestion de la fiche, des captures successives et des informations du Pokémon.
  - `ui` : affichage, portrait, insignes et déplacement de la fenêtre.
  - `release` : relâchement, localisation et protections.
  - `mixin` : injections ciblées dans Minecraft et Cobblemon.
- `src/main/resources` : métadonnées, traductions et cadre de l'interface.
- `src/test/java` : tests et vérifications des intégrations.

La détection s'appuie sur le stockage synchronisé par Cobblemon. Un ajout personnalisé par un serveur peut être interprété comme une capture. Le serveur reste responsable d'autoriser ou de refuser un relâchement.

## Licence

All-Rights-Reserved. Consulter [LICENSE](LICENSE) et [les crédits tiers](THIRD_PARTY_NOTICES.md).
