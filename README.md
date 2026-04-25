# Système Distribué en Java : Protocole 2PC

Ce projet implémente une simulation de système distribué en Java, où des machines partagent et transforment des ressources via des transactions atomiques pilotées par un protocole de validation à deux phases (Two-Phase Commit - 2PC).

## Compétences techniques illustrées

Ce projet met en pratique des concepts avancés de l'écosystème Java et de l'architecture logicielle :

* **Systèmes Distribués :** Implémentation from scratch du protocole **Two-Phase Commit (2PC)** garantissant l'atomicité et la cohérence des transactions réparties sur plusieurs nœuds du réseau.
* **Communication Réseau :** Architecture Client/Serveur TCP gérée via l'API `java.net.Socket`. Élaboration d'un protocole de communication par échange de flux textuels (`DataInputStream` / `DataOutputStream`).
* **Programmation Concurrente (Multithreading) :**
  * Traitement asynchrone des requêtes entrantes via des pools de threads (`Executors.newCachedThreadPool()`).
  * Résolution des problèmes d'accès concurrents (*race conditions*) par l'utilisation de verrous (`synchronized`) lors de la réservation des ressources.
  * Utilisation experte de collections *Thread-Safe* (`ConcurrentHashMap`) pour maintenir l'état des données.
* **Outils Java Avancés :** 
  * Orchestration et déploiement simulé en lançant plusieurs machines virtuelles Java (JVM) à la volée avec `ProcessBuilder`.
  * Configuration complète de l'API `java.util.logging` pour tracer l'exécution (fichiers `.log` et console) et monitorer le ratio d'efficacité des transactions.

## Compilation : 
Assurez-vous d'avoir le JDK installé. Placez tous les fichiers .java dans le même dossier.
Tapez cette commande dans le terminal : ```javac *.java```

## Execution : 
Il existe deux méthodes pour lancer le système.

### 1ère méthode : Test automatisé
Une classe Test.java est fournie pour lancer automatiquement un scénario complet sur une seule machine via ProcessBuilder.
Tapez cette commande dans le terminal : ```java Test``` 

Le scénario est le suivant :
Lance 2 machines sur le port 6001 et 6002 et un Executor pour une durée de 10 secondes avant d'arrêter le processus.
Les logs s'afficheront dans le terminal et sont sauvegardés dans 3 fichiers logs.

### 2ème méthode : Lancement manuel
Pour tester sur plusieurs machines ou terminaux différents :
#### A - Lancer une machine :
```bash
java Machine --port <PORT> --resource "(<NOM>,<QUANTITE>)" --resource "(<NOM>,<QUANTITE>)"
```
#### B - Lancer un executor :
```bash
java Executor --reaction "<REGLE>" --machine "<IP:PORT>" --machine "<IP:PORT>"
```
## Auteur :
* **CHARAF Hassan**
