# Light-Tron-Cycle-2D

## Auteurs
- Romain Durussel 
- Abram Zweifel

HEIG-VD, Class C, 2025–2026

## Table des matières

- [Règles du jeu](#règles-du-jeu)
- [Le protocole applicatif](#le-protocole-applicatif)
    - [Aperçu](#aperçu)
    - [Protocole de transport](#protocole-de-transport)
    - [Messages](#messages)
        - [Connexion et annonce du joueur](#connexion-et-annonce-du-joueur-handshake-et-session)
        - [Déclaration de disponibilité (READY)](#déclaration-de-disponibilité)
        - [Début d’une partie (GAME_START)](#début-dune-partie)
        - [Envoi des entrées (INPUT)](#envoi-des-entrées)
        - [État du jeu (STATE)](#état-du-jeu)
        - [Fin de partie (GAME_END)](#fin-de-partie)
        - [Gestion des erreurs (ERROR)](#gestion-des-erreurs)
    - [Exemples](#exemples)


## Règles du jeu

Le jeu s’inspire du concept de Tron Light Cycle en 2D.
Deux joueurs se déplacent sur une grille dans l’une des quatre directions possibles : UP, DOWN, LEFT ou RIGHT.

Chaque joueur laisse derrière lui une traînée qui occupe les cases du plateau au fur et à mesure de son déplacement.
Une manche se termine immédiatement lorsqu’un joueur entre en collision, que ce soit :

- avec un mur de la grille,

- avec sa propre traînée,

- avec la traînée de l’adversaire.

Le dernier joueur encore en vie est déclaré vainqueur de la partie.
En cas de collision simultanée (par exemple, les deux joueurs se percutent tête-à-tête ou arrivent sur la même case), la manche se conclut par un DOUBLE_KO, sans vainqueur.

## Le protocole applicatif
### Aperçu

Cette section définit un protocole d’application pour un jeu multijoueur en ligne inspiré de Tron Light Cycle. 
Deux clients se connectent à un serveur autoritaire qui simule la partie sur une grille 2D. 
À chaque tick de la simulation, le serveur applique les entrées de direction, déplace les joueurs, met à jour les traînées, détecte les collisions. 
Une manche se termine lorsqu’un joueur entre en collision (avec un mur ou une traînée).

### Protocole de transport

Le protocole d’application est un protocole textuel utilisant TCP comme transport. Le protocole TCP est utilisé (fiable et orienté connexion).
Le port utilisé par défaut est 2222 (configurable via la CLI).

Les messages sont envoyés en texte brut UTF-8, un par ligne. Chaque message est donc une ligne terminée par \n.

Les messages ont la forme générale :

```
COMMANDE param1 param2 ... paramN
```

Le serveur peut à tout moment renvoyer un message pour signaler une erreur de protocole ou de logique. En cas d’erreur grave, le serveur peut ensuite fermer la connexion.
Le serveur est autoritaire : seul son état fait foi.
Les clients envoient des entrées utilisateur (changement de direction) et rendent l’état reçu.

### Messages

#### Connexion et annonce du joueur (handshake et session)

Le client annonce son nom du joueur au serveur. 
Le serveur répond en attribuant un identifiant unique au joueur dans la partie.

Request
```
HELLO <playerName>
```

- HELLO
  - playerName : pseudo du joueur, sans espace (par exemple Alice).

Response
```
WELCOME <playerId>
ERROR <code> <message>
```

- WELCOME signifie que le serveur accepte la connexion et la version de protocole.
  - playerId : identifiant unique attribué au joueur (par exemple P1).

- ERROR signifie que le serveur refuse la connexion ou la version. 
  - code : entier représentant le type d’erreur (voir section Error handling plus bas). 
  - message : court texte expliquant l’erreur.


#### Déclaration de disponibilité

Une fois connecté, le client informe le serveur qu’il est prêt à commencer une partie. 
Le serveur ne commencera une partie que lorsqu’il aura reçu un message READY de chacun des deux joueurs.

Request
```
READY
```

Ce message ne contient pas de paramètre : il signifie simplement « je suis prêt ».

Response
Aucune réponse directe n’est envoyée.
Lorsque les deux joueurs sont prêts, le serveur enverra un message GAME_START (voir plus bas) à tous les clients pour indiquer le début de la partie.


#### Début d’une partie

Le serveur informe les clients qu’une nouvelle partie commence. Il fournit les positions initiales et les directions des joueurs.

Request
Aucun client n’envoie ce message. GAME_START est toujours envoyé par le serveur.

Response (diffusée par le serveur à tous les joueurs)
```
GAME_START <matchId> <p1Id> <p1x> <p1y> <p1dir> <p2Id> <p2x> <p2y> <p2dir>
```

- matchId : identifiant de la partie.
- p1Id, p2Id : identifiants des joueurs (par exemple P1, P2).
- p1x p1y : position initiale du joueur 1
- p1dir : direction initiale du joueur 1 (UP, DOWN, LEFT ou RIGHT).
- p2x p2y : position initiale du joueur 2.
- p2dir : direction initiale du joueur 2.

Les traînées sont vides au début de chaque manche ; seuls les deux points de départ des joueurs sont occupés par leur tête.


#### Envoi des entrées

Le client envoie au serveur la direction que le joueur souhaite prendre. 
Le serveur applique au plus une direction par joueur et par tick, en utilisant la dernière direction reçue à temps.

Request
```
INPUT <direction>
```

- direction : une des valeurs suivantes : UP, DOWN, LEFT, RIGHT.

Le client peut envoyer plusieurs INPUT très rapprochés ; le serveur choisira la dernière direction reçue avant le prochain tick.

Response
Il n’y a pas de réponse immédiate à un message INPUT.
L’effet d’un INPUT est visible dans les messages STATE ultérieurs envoyés par le serveur : la position et la direction du joueur seront mises à jour.


#### État du jeu

Le serveur envoie régulièrement un résumé de l’état actuel de la manche à tous les clients. 
Ce message contient le numéro de tick, une description simplifiée des positions des joueurs et des traînées.

Pour garder le protocole lisible, les informations complexes (positions des traînées) sont envoyées dans une forme compacte, par exemple sous forme de listes séparées par des virgules.

Request
Les clients n’envoient jamais de message STATE. Ce message est toujours émis par le serveur.

Response (diffusée par le serveur à tous les joueurs)
```
STATE <matchId> <tick> <phase> <players> <trails>
```

- matchId : identifiant de la partie.
- tick : numéro du tick dans la manche (commence à 0 au début de chaque manche).
- phase : phase actuelle du jeu pour la manche. Les valeurs possibles sont :
  - LOBBY : la partie n’a pas encore commencé (en attente des joueurs prêts). 
  - RUNNING : la partie est en cours. 
  - GAME_OVER : la partie est terminée (un message GAME_END a été ou va être envoyé).
- players : liste des joueurs, au format : playerId:x:y:dir:alive,playerId2:x2:y2:dir2:alive2 
- où alive vaut 1 si le joueur est en vie et 0 s’il est déjà en collision.
- trails : liste des cases occupées par les traînées, au format : x1:y1,x2:y2,x3:y3,...
Si aucune traînée n’est présente (par exemple au début de la manche), ce champ doit être '-' lorsque aucune traînée n’est présente.

#### Fin de partie

Lorsqu'un joueur entre en collision, le serveur annonce la fin de la partie et le gagnant final.

Request
Les clients n’envoient pas ce message. GAME_END est exclusivement envoyé par le serveur.

Response (diffusée par le serveur à tous les joueurs)
```
GAME_END <reason> <winnerId>
```
- reason : raison de la fin de manche. Les valeurs possibles sont :
  - COLLISION : un joueur est entré en collision avec un mur ou une traînée. 
  - DOUBLE_KO : les deux joueurs entrent en collision en même temps, par exemple en arrivant sur la même case ou en se percutant tête-à-tête. 
  - DISCONNECT : un joueur s’est déconnecté pendant la manche.
- winnerId : identifiant du joueur qui a gagné la partie. En cas de DOUBLE_KO (égalité), cela doit être '-'.

Après ce message, le serveur peut fermer la connexion ou, selon l’implémentation, attendre de nouveaux READY pour démarrer une nouvelle partie.


#### Gestion des erreurs

Le serveur utilise ce message pour signaler un problème de protocole, un message invalide ou une action non autorisée. En fonction de la gravité de l’erreur, le serveur peut continuer à accepter des messages ou fermer la connexion.

Request
Les clients n’envoient pas ERROR. Ce message est uniquement envoyé par le serveur.

Response (du serveur vers le client)
```
ERROR <code> <message>
```

- code : entier indiquant le type d’erreur.
- message : court texte explicatif.

Les codes d’erreur suivants sont définis :

1 : version non supportée (HELLO avec une version de protocole que le serveur ne gère pas).

2 : message invalide (syntaxe incorrecte ou paramètre manquant).

3 : action non autorisée (par exemple INPUT envoyé alors qu’aucune manche n’est en cours).

4 : partie pleine (le serveur ne peut pas accepter un nouveau joueur).

5 : erreur interne du serveur.

Le client doit au minimum afficher le message d’erreur à l’utilisateur.

### Exemples
#### TODO