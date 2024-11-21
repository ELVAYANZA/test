# Explorateur de fichiers

Un explorateur de fichiers intuitif et puissant, permettant de naviguer, visualiser et gérer les fichiers et dossiers sur votre appareil ou  vos périphériques connectés.

## Fonctionnalités

- Navigation dans les dossiers locaux et périphériques connectés:
- Création; suppression, et renommage de fichiers/dossiers.
- Recherche rapide par nom ou extension.
- Prise en charge des appareils externes (USB).
- Interface utilistateur intuitive avec un arbre (JTree) et une table (Affichage mode grille et mode list).

## Prérequis

- Java 1.8
- IDE Eclipse et VS code
- [jMTPe](https://github.com/jmtp/jmtp) pour gérer les périphériques MTP:

## Installation

1. Clonez le dépôt : git@github.com:elishamavayanza/file-manager.git

### **5. Utilisation**

Description de l'utilisation :

1. Lancez l'applicaton.
2. Naviguez dans vos fichiers via l'arborescence.
3. Utilisez le menu contextuel (clic droit) pour effectuer des operation (copier, couper, coller, renommer, etc.).
4. Connectez un péripherique pour affichier son contenu.

## Captures d'ecran
![Explorateur principal mode grille](./image/explorateur1.png)
*Vue principale de l'explorateur de fichiers en mode grille.*

![Explorateur principal mode list](./image/explorateur2.png)
*Vue principale de l'explorateur de fichiers en mode list.*

## Architecture du Projet
- **src/**
- **lib/** : Dépendances externes (comme jMTPe, flatlaf).

## Auteurs
- [ELISHAMA VAYANZA](https://github.com/elishamavayanza) - Etudiant en Génie Informatiaue

