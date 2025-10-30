# MyRealAI

Application mobile d'intelligence artificielle multifonctionnelle développée avec Kotlin Multiplatform et Jetpack Compose.

Cette application fonctionne avec ce repository : https://github.com/realdev-fr/MyAIBackend

> **Note importante** : Ce projet n'a été testé que sur **Android**. La compatibilité avec iOS n'est pas garantie.

## Description

MyRealAI est une application mobile qui intègre plusieurs fonctionnalités d'intelligence artificielle, notamment la traduction, le chat conversationnel, l'IA locale embarquée et la reconnaissance vocale.

## Fonctionnalités

### 1. **Traduction** (Translate)
Traduction de texte entre différentes langues en utilisant des API d'IA.

### 2. **Chat** (Chat)
Interface de chat conversationnel avec une IA en ligne pour des discussions intelligentes.

### 3. **IA Locale** (Local Chat)
Cette fonctionnalité unique permet d'exécuter des modèles d'IA **directement sur votre appareil Android**, sans connexion Internet.

#### Caractéristiques de l'IA locale :
- Fonctionne entièrement hors ligne
- Utilise des modèles optimisés pour smartphones
- Deux modes disponibles :
  - **Rapide** : Réponses plus rapides avec moins de réflexion
  - **Réflexion** : Réponses plus élaborées et réfléchies

#### Configuration requise :
- Smartphone Android avec **minimum 12 GB de RAM**
- Modèles IA compatibles avec :
  - **TensorFlow Lite** (.task)
  - Format **int4** (quantification 4 bits pour optimisation mobile)

### 4. **Speak** (Reconnaissance vocale)
Interaction vocale avec l'IA pour des conversations mains libres. Cette reconnaissance fonctionne avec le serveur qui fait fonctionner un petit model de Speech-to-Text, donc les résultats ne sont pas forcément convainquants.

## Installation des modèles IA locaux

### Sources des modèles

Les modèles compatibles sont disponibles sur Kaggle :

- **Base de données principale** : https://www.kaggle.com/models
- **Modèles Gemma (TensorFlow Lite)** : https://www.kaggle.com/models/google/gemma/tfLite
- **Modèle Gemma 3N (int4)** : https://www.kaggle.com/models/google/gemma-3n/TfLite/gemma-3n-e4b-it-int4/1

### Modèles recommandés

1. **Gemma 2B IT (GPU int4)** - Format `.bin`
   - Taille : ~1.3 GB
   - Optimisé pour GPU mobile

2. **Gemma 3N E4B IT (int4)** - Format `.task`
   - Taille : ~4.1 GB
   - Meilleure qualité de réponse

### Procédure de transfert des modèles

#### Prérequis
- Avoir installé ADB (Android Debug Bridge)
- Activer le mode développeur sur votre appareil Android
- Activer le débogage USB

#### Étape 1 : Préparation du répertoire
```bash
# Nettoyer les anciens modèles (si nécessaire)
adb shell rm -r /data/local/tmp/llm/

# Créer le répertoire de transfert
adb shell mkdir -p /data/local/tmp/llm/
```

#### Étape 2 : Transfert d'un modèle .bin (Gemma 2B)
```bash
# Transférer le fichier vers /data/local/tmp/
adb push gemma-2b-it-gpu-int4.bin /data/local/tmp/

# Copier le fichier dans le répertoire de l'application
adb shell run-as cloud.realdev.myai cp /data/local/tmp/gemma-2b-it-gpu-int4.bin files/model_version.bin
```

#### Étape 3 : Transfert d'un modèle .task (Gemma 3N)
```bash
# Transférer le fichier vers /data/local/tmp/
adb push gemma-3n-E4B-it-int4.task /data/local/tmp/

# Copier le fichier dans le répertoire de l'application
adb shell run-as cloud.realdev.myai cp /data/local/tmp/gemma-3n-E4B-it-int4.task files/model_version.task
```

#### Étape 4 : Vérification
```bash
# Vérifier que les modèles sont bien installés
adb shell run-as cloud.realdev.myai ls -lh files/
```

**Résultat attendu :**
```
total 5.3M
-rwxrwxrwx 1 u0_a848 u0_a848 1.2G model_version.bin
-rwxrwxrwx 1 u0_a848 u0_a848 4.1G model_version.task
```

### Notes importantes sur les modèles

- Les noms de fichiers **doivent être** `model_version.bin` ou `model_version.task`
- Les modèles en format `.bin` sont généralement plus rapides mais moins précis
- Les modèles en format `.task` (TensorFlow Lite) offrent de meilleures performances
- Le transfert peut prendre plusieurs minutes selon la taille du modèle
- Assurez-vous d'avoir suffisamment d'espace de stockage sur votre appareil

## Architecture technique

### Stack technologique

- **Langage** : Kotlin
- **Framework** : Kotlin Multiplatform (KMP)
- **UI** : Jetpack Compose
- **Navigation** : Androidx Navigation 3
- **Client HTTP** : Ktor
- **IA** : Google GenAI SDK + TensorFlow Lite
- **Sérialisation** : Kotlinx Serialization

### Structure du projet

```
MyRealAI/
├── composeApp/
│   ├── src/
│   │   ├── androidMain/          # Code spécifique Android
│   │   ├── commonMain/           # Code partagé multiplateforme
│   │   │   ├── kotlin/
│   │   │   │   └── cloud/realdev/myai/
│   │   │   │       ├── models/    # Modèles de données
│   │   │   │       ├── views/     # Interfaces utilisateur
│   │   │   │       └── viewmodels/ # Logique métier
│   │   └── iosMain/              # Code spécifique iOS
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

### Écrans principaux

1. **HomeView** : Menu principal avec accès à toutes les fonctionnalités
2. **TranslateView** : Interface de traduction
3. **ChatView** : Chat avec IA en ligne
4. **LocalChatView** : Chat avec IA locale
5. **SpeakView** : Interface de reconnaissance vocale

## Configuration et lancement

### Prérequis

- Android Studio Koala ou supérieur
- JDK 11 ou supérieur
- SDK Android (minSdk: voir `gradle/libs.versions.toml`)

### Compilation

```bash
# Compiler l'application Android
./gradlew assembleDebug

# Installer sur un appareil connecté
./gradlew installDebug
```

### Lancement

```bash
# Lancer directement sur un appareil
./gradlew :composeApp:installDebug
adb shell am start -n cloud.realdev.myai/.MainActivity
```

## Configuration de l'API

L'application nécessite une clé API pour les fonctionnalités de chat en ligne. Configurez vos clés API dans le fichier approprié (voir la classe `Constants.kt`).

## Dépannage

### Problèmes courants avec l'IA locale

1. **"Model not found"** : Vérifiez que les modèles sont bien nommés `model_version.bin` ou `model_version.task`
2. **Application crash au chargement** : Assurez-vous que votre appareil a suffisamment de RAM (12 GB minimum)
3. **Réponses lentes** : Utilisez le mode "Rapide" ou un modèle plus léger (.bin)
4. **Erreur de transfert ADB** : Vérifiez que le débogage USB est activé

### Commandes de débogage utiles

```bash
# Voir les logs de l'application
adb logcat | grep MyRealAI

# Vérifier l'espace de stockage disponible
adb shell df -h

# Lister les fichiers de l'application
adb shell run-as cloud.realdev.myai ls -lh files/
```

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou une pull request.

## Licence

[Ajouter votre licence ici]

## Contact

[Ajouter vos informations de contact ici]

---

**Développé avec ❤️ en Kotlin Multiplatform**