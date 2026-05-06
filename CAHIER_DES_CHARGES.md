# Cahier des Charges — HotelSurvey Application
**Projet :** Application d'enquête de satisfaction hôtelière  
**Client :** Hasdrubal Thalassa & Spa — Yasmine Hammamet  
**Version :** 1.0  
**Date :** Mai 2026  
**Statut :** En cours de développement  

---

## 1. Présentation du Projet

### 1.1 Contexte
L'hôtel Hasdrubal Thalassa & Spa souhaite moderniser la collecte d'avis clients en remplaçant le formulaire papier par une application Android installée sur les téléviseurs des chambres. Les téléviseurs sont des boîtiers Android gérés à distance via un broker MQTT (Mosquitto). Le service existant `BoitierSystemService` reçoit les commandes MQTT et lance les applications.

### 1.2 Objectif
Développer une application Android (`HotelSurveyApp`) qui :
- S'affiche sur le téléviseur de la chambre du client
- Guide le client à travers une enquête de satisfaction
- Envoie les résultats vers un serveur via API REST
- Fonctionne exclusivement avec une télécommande (pas de tactile)

### 1.3 Périmètre
| Dans le périmètre | Hors périmètre |
|-------------------|----------------|
| Application Android TV (télécommande) | Application mobile (smartphone/tablette) |
| Enquête de satisfaction chambre | Gestion back-office des résultats |
| Envoi API REST | Tableau de bord statistique |
| Lancement via MQTT | Authentification utilisateur |
| Sauvegarde locale de progression | Gestion multi-hôtels |

---

## 2. Acteurs du Système

| Acteur | Rôle | Interaction |
|--------|------|-------------|
| **Client** | Répond à l'enquête | Télécommande TV |
| **Staff hôtel** | Surveille les réponses | Dashboard (hors périmètre) |
| **Système MQTT** | Lance l'application sur la TV | Commande `OPEN:com.hotel.survey` |
| **Serveur API** | Reçoit et stocke les réponses | HTTP POST JSON |

---

## 3. Exigences Fonctionnelles

---

### F1 — Affichage de l'identifiant du téléviseur (IP)

**Description :** À chaque lancement de l'application, l'adresse IP du téléviseur doit être affichée afin de permettre à l'équipe hôtelière d'identifier quelle chambre a soumis quelle réponse.

**Règles de gestion :**
- Afficher l'**IP LAN** (réseau local) en priorité — disponible instantanément
- Afficher l'**IP WAN** (internet) en second — récupérée via requête HTTP asynchrone
- Si l'IP WAN est indisponible (pas de connexion), afficher uniquement l'IP LAN
- L'IP est affichée discrètement en bas de l'écran d'accueil (petite taille, non intrusive)
- L'IP LAN et l'IP WAN sont également **incluses dans le payload envoyé à l'API**

**Format d'affichage :**
```
LAN: 192.168.1.45   |   WAN: 41.226.xxx.xxx
```

**Données envoyées à l'API :**
```json
{
  "device_id": "abc123def456",
  "lan_ip": "192.168.1.45",
  "wan_ip": "41.226.12.34",
  ...
}
```

---

### F2 — Écran d'accueil avec choix de démarrage

**Description :** Lors du lancement de l'application, le client doit avoir le choix entre trois options.

**Interface :**
```
┌──────────────────────────────────────────────────────┐
│  [Logo Hasdrubal]    HASDRUBAL THALASSA & Spa         │
│                                                        │
│  We would love to hear about your stay!               │
│                                                        │
│  ┌─────────────────┐                                  │
│  │  ★ Start Now    │  ← focus par défaut              │
│  └─────────────────┘                                  │
│  ┌─────────────────┐                                  │
│  │  ⏱ Later       │                                  │
│  └─────────────────┘                                  │
│  ┌─────────────────┐                                  │
│  │  ✕ Not Now      │                                  │
│  └─────────────────┘                                  │
│                                                        │
│  LAN: 192.168.1.45  |  WAN: 41.226.12.34              │
└──────────────────────────────────────────────────────┘
```

**Navigation :** `↑ ↓` pour changer le bouton sélectionné — `OK` pour confirmer

**Comportement des 3 options :**

| Option | Comportement |
|--------|-------------|
| **Start Now** | Lance l'enquête immédiatement. Si une progression sauvegardée existe, propose de reprendre ou recommencer. |
| **Later** | Ferme l'application. Elle peut être relancée par MQTT à tout moment. La progression éventuellement sauvegardée est conservée. |
| **Not Now** | Ferme l'application et enregistre localement un flag `survey_declined`. Le service MQTT ne relancera pas l'enquête pour une durée configurable (défaut : 24 heures). |

**Règle supplémentaire :** Si une réponse complète a déjà été soumise pour cette session (même `device_id` + même jour), l'option "Start Now" est remplacée par "Survey completed — Thank you!" en lecture seule. L'enquête ne peut être soumise qu'une fois par jour par appareil.

---

### F3 — Déroulement de l'enquête

**Description :** L'enquête comporte **20 questions** en anglais, organisées par catégorie, chacune notée de 1 à 5 étoiles. Le client peut passer les questions correspondant à des services qu'il n'a pas utilisés.

**Liste des questions (ordre fixe) :**

| # | Catégorie | Sous-catégorie | Question |
|---|-----------|----------------|----------|
| 1 | YOUR ROOM | — | How do you rate your room? |
| 2 | FRONT DESK | — | How do you rate the reception? |
| 3 | LA TOPAZE | Breakfast | Quality of the breakfast buffet |
| 4 | LA TOPAZE | Breakfast | Service |
| 5 | IL DELFINO | Restaurant | Quality of meals |
| 6 | IL DELFINO | Restaurant | Service |
| 7 | LE GOURMET | Restaurant | Quality of meals |
| 8 | LE GOURMET | Restaurant | Service |
| 9 | L'OLIVIER | Restaurant | Quality of meals |
| 10 | L'OLIVIER | Restaurant | Service |
| 11 | LE VENUS | Restaurant | Quality of meals |
| 12 | LE VENUS | Restaurant | Service |
| 13 | LA CASCADE | — | How do you rate La Cascade? |
| 14 | LA BRISE | — | How do you rate La Brise? |
| 15 | LOBBY BAR | — | How do you rate the Lobby Bar? |
| 16 | ROOM SERVICE | — | How do you rate the Room Service? |
| 17 | SPA CENTER | Thalassotherapy | Quality of welcome |
| 18 | SPA CENTER | Thalassotherapy | Quality of treatments |
| 19 | LAUNDRY | — | How do you rate the Laundry service? |
| 20 | OVERALL STAY | — | How do you rate your overall stay? |

**Échelle de notation :**

| Étoiles | Label |
|---------|-------|
| ★☆☆☆☆ (1) | Inadequate |
| ★★☆☆☆ (2) | Average |
| ★★★☆☆ (3) | Good |
| ★★★★☆ (4) | Very Good |
| ★★★★★ (5) | Excellent |

**Navigation télécommande durant l'enquête :**

| Touche | Action |
|--------|--------|
| `◄` | Diminuer la note (-1 étoile) |
| `►` | Augmenter la note (+1 étoile) |
| `OK` / `ENTER` | Confirmer la note et passer à la suivante |
| `↑` (DPAD_UP) | Passer la question (service non utilisé) |
| `BACK` | Revenir à la question précédente |
| `HOME` / touche dédiée | Mettre en pause et sauvegarder |

---

### F4 — Pause et Reprise de l'enquête

**Description :** Le client peut interrompre l'enquête à tout moment. Sa progression est sauvegardée automatiquement. Lors du prochain lancement, l'application propose de reprendre là où il s'était arrêté.

#### F4.1 — Sauvegarde automatique de la progression

- Chaque réponse confirmée est sauvegardée **immédiatement** en local (`SharedPreferences`)
- La sauvegarde contient :
  - Le numéro de la question en cours
  - Les réponses déjà données (question_id → rating)
  - L'horodatage du début de l'enquête
  - L'identifiant de l'appareil

**Déclencheurs de sauvegarde :**
- Confirmation d'une réponse (OK)
- Passage d'une question (↑)
- Fermeture de l'application (onPause / onStop)
- Appui sur "Later" depuis l'écran d'accueil

#### F4.2 — Reprise de la progression

Quand l'application est relancée et qu'une progression sauvegardée existe, l'écran d'accueil affiche :

```
┌──────────────────────────────────────────────────────┐
│                                                        │
│  You have an unfinished survey.                       │
│  Progress: Question 7 of 20 (35%)                    │
│                                                        │
│  ┌──────────────────┐  ┌──────────────────┐          │
│  │  ↩ Continue      │  │  ↺ Start Over    │          │
│  └──────────────────┘  └──────────────────┘          │
│                                                        │
└──────────────────────────────────────────────────────┘
```

| Option | Comportement |
|--------|-------------|
| **Continue** | Reprend à la question sauvegardée, avec les réponses précédentes conservées |
| **Start Over** | Efface la progression sauvegardée et recommence depuis la question 1 |

#### F4.3 — Expiration de la progression sauvegardée

- Une progression sauvegardée **expire après 24 heures**
- Si elle a expiré, elle est automatiquement effacée au lancement
- L'écran d'accueil revient au mode normal (F2)

#### F4.4 — Soumission partielle

- Si le client n'a répondu qu'à une partie des questions et choisit de soumettre (atteint la fin), les questions passées (`skipped: true`) sont incluses dans le payload avec `rating: 0`
- Une soumission nécessite au minimum que **les questions 1 (Room) et 20 (Overall)** aient une note (sinon, un message d'avertissement s'affiche)

---

### F5 — Envoi des résultats vers l'API REST

**Description :** À la fin de l'enquête, les résultats sont envoyés en arrière-plan vers le serveur de l'hôtel.

**Endpoint :**
```
POST {BASE_URL}/survey/submit
Content-Type: application/json
```

**Payload JSON :**
```json
{
  "submitted_at": "2026-05-06T14:30:00Z",
  "device_id": "a3f4b2c1d8e9",
  "lan_ip": "192.168.1.45",
  "wan_ip": "41.226.12.34",
  "survey_started_at": "2026-05-06T14:15:00Z",
  "duration_seconds": 900,
  "responses": [
    {
      "question_id": 1,
      "category": "YOUR ROOM",
      "subcategory": null,
      "question": "How do you rate your room?",
      "rating": 4,
      "skipped": false
    },
    {
      "question_id": 19,
      "category": "LAUNDRY",
      "subcategory": null,
      "question": "How do you rate the Laundry service?",
      "rating": 0,
      "skipped": true
    }
  ]
}
```

**Comportement en cas d'erreur réseau :**
- L'application navigue vers l'écran de remerciement **sans attendre** la réponse du serveur
- L'envoi se fait en arrière-plan (thread séparé)
- En cas d'échec : la réponse est stockée localement dans une file d'attente (`pending_surveys.json`)
- Un mécanisme de retry tente de renvoyer les réponses en attente au prochain lancement de l'application

---

### F6 — Écran de remerciement

**Description :** Après soumission, un écran de remerciement s'affiche pendant 5 secondes puis l'application se ferme automatiquement (`finishAffinity()`).

**Contenu :**
- Icône de validation (✓)
- Message : "Thank You! — Your satisfaction is our priority."
- Fermeture automatique après 5 secondes
- `OK` ou `BACK` ferme immédiatement

---

### F7 — Intégration MQTT (BoitierSystemService)

**Description :** L'application est lancée depuis le service MQTT existant.

**Commande MQTT pour lancer l'enquête :**
```
Topic  : maison/android/actions
Payload: OPEN:com.hotel.survey
```

**Flux complet :**
```
Broker MQTT → BoitierSystemService (écoute) → OPEN:com.hotel.survey → WelcomeActivity
```

**Aucune modification** du service MQTT existant n'est nécessaire pour le lancement de base.

---

## 4. Exigences Non-Fonctionnelles

### 4.1 Performance
| Critère | Valeur cible |
|---------|-------------|
| Démarrage de l'application | < 2 secondes |
| Temps de réponse de l'UI | < 100 ms après appui touche |
| Envoi API (timeout) | 10 secondes max |
| Taille de l'APK | < 10 Mo |

### 4.2 Fiabilité
- L'application fonctionne **sans connexion internet** (sauf pour l'envoi API et l'IP WAN)
- Les données ne sont jamais perdues (sauvegarde locale avant envoi)
- L'application ne plante pas si le serveur API est indisponible

### 4.3 Compatibilité
| Critère | Valeur |
|---------|--------|
| Android minimum | API 21 (Android 5.0) |
| Orientation | Paysage uniquement |
| Résolution cible | 1280×720 (HD), 1920×1080 (Full HD) |
| Navigation | Télécommande (D-pad) — tactile optionnel |
| Leanback (Android TV) | Compatible (non requis) |

### 4.4 Sécurité
- L'URL de l'API REST est configurable (pas en dur dans le code)
- Aucune donnée personnelle n'est collectée (pas de nom, email, numéro de chambre)
- Les communications réseau utilisent HTTPS en production

---

## 5. Architecture Technique

### 5.1 Stack technologique
| Composant | Technologie |
|-----------|-------------|
| Langage | Java 17 |
| Build | Gradle 9.0 / AGP 8.9.0 |
| Réseau | Retrofit 2.9.0 + OkHttp 4.12.0 |
| Serialisation | Gson 2.10.1 |
| Persistance locale | SharedPreferences |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 35 (Android 15) |

### 5.2 Structure des écrans

```
WelcomeActivity          → Accueil + choix (Start / Later / Not Now)
     │
     ├── [progression sauvegardée?] → ResumeDialogActivity  (Continue / Start Over)
     │
     └── SurveyActivity           → Questions 1 à 20 (avec sauvegarde auto)
              │
              └── ThankYouActivity → Remerciement + fermeture auto (5s)
```

### 5.3 Structure des fichiers du projet

```
com.hotel.survey/
├── WelcomeActivity.java        — Accueil + détection reprise + choix
├── SurveyActivity.java         — Enquête + navigation + sauvegarde auto
├── ThankYouActivity.java       — Écran de remerciement
├── model/
│   ├── Question.java           — Modèle d'une question
│   ├── QuestionResponse.java   — Réponse à une question (pour API)
│   └── SurveyResult.java       — Résultat complet (pour API)
├── api/
│   ├── ApiClient.java          — Configuration Retrofit
│   └── ApiService.java         — Interface endpoint POST
├── storage/
│   ├── SurveyStateManager.java — Sauvegarde/reprise progression (SharedPreferences)
│   └── PendingSubmitQueue.java — File d'attente réponses non envoyées
└── utils/
    ├── NetworkUtils.java       — Récupération IP LAN / WAN
    └── DeviceUtils.java        — Identifiant appareil (ANDROID_ID)
```

### 5.4 Modèle de données local (SharedPreferences)

| Clé | Type | Description |
|-----|------|-------------|
| `survey_in_progress` | boolean | Indique une progression sauvegardée |
| `survey_current_index` | int | Index de la question en cours |
| `survey_started_at` | long | Timestamp de début (ms) |
| `survey_answers` | String (JSON) | Map question_id → rating |
| `survey_declined_until` | long | Timestamp d'expiration du flag "Not Now" |
| `survey_submitted_today` | String | Date (yyyy-MM-dd) de la dernière soumission |

---

## 6. Maquettes des Écrans

### 6.1 Écran d'accueil (sans progression sauvegardée)
```
┌──────────────────┬─────────────────────────────────────────────┐
│  [Logo]          │                                             │
│  HASDRUBAL       │         Welcome!                           │
│  THALASSA & Spa  │   Your satisfaction is our priority.       │
│  YASMINE         │                                             │
│  HAMMAMET        │   ┌─────────────────────────────────┐     │
│                  │   │        ★  Start Now              │ ◄── │
│  ──────────────  │   └─────────────────────────────────┘     │
│  Guest           │   ┌─────────────────────────────────┐     │
│  Satisfaction    │   │        ⏱  Later                  │     │
│  Survey          │   └─────────────────────────────────┘     │
│                  │   ┌─────────────────────────────────┐     │
│                  │   │        ✕  Not Now                │     │
│                  │   └─────────────────────────────────┘     │
│                  │                                             │
│                  │  LAN: 192.168.1.45  |  WAN: 41.226.xx.xx  │
└──────────────────┴─────────────────────────────────────────────┘
```

### 6.2 Écran d'accueil (avec progression sauvegardée)
```
┌──────────────────┬─────────────────────────────────────────────┐
│  [Logo]          │                                             │
│  HASDRUBAL       │   You have an unfinished survey.           │
│  THALASSA & Spa  │   Progress: Question 7 of 20 ████████░░░  │
│                  │                                             │
│                  │   ┌────────────────┐ ┌────────────────┐   │
│                  │   │  ↩ Continue    │ │  ↺ Start Over  │   │
│                  │   └────────────────┘ └────────────────┘   │
│                  │                                             │
│                  │  LAN: 192.168.1.45  |  WAN: 41.226.xx.xx  │
└──────────────────┴─────────────────────────────────────────────┘
```

### 6.3 Écran enquête
```
┌──────────────────┬─────────────────────────────────────────────┐
│  [Logo mini]     │ Progress  7 / 20                            │
│  HASDRUBAL       │ ████████████░░░░░░░░░░░░░░░░░░░░░░         │
│                  │─────────────────────────────────────────────│
│  ──────────      │                                             │
│  IL DELFINO      │     How do you rate the quality            │
│  Restaurant      │           of meals?                        │
│                  │                                             │
│  ──────────      │      ★    ★    ★    ★    ☆               │
│                  │                                             │
│  Progress        │            Very Good                        │
│  7 / 20          │                                             │
│  ████████░       │─────────────────────────────────────────────│
│                  │  ◄ ► select  |  OK confirm  |  ↑ skip      │
└──────────────────┴─────────────────────────────────────────────┘
```

### 6.4 Écran remerciement
```
┌──────────────────┬─────────────────────────────────────────────┐
│  [Logo]          │                                             │
│  HASDRUBAL       │                  ✓                         │
│  THALASSA & Spa  │                                             │
│  YASMINE         │           Thank You!                       │
│  HAMMAMET        │   Your satisfaction is our priority.       │
│                  │                                             │
│                  │  We hope to welcome you again soon.        │
│                  │                                             │
│                  │  Closing automatically in 5 seconds...     │
└──────────────────┴─────────────────────────────────────────────┘
```

---

## 7. Plan de Développement

### Phase 1 — Foundation (Actuel ✓)
- [x] Structure du projet Android
- [x] 20 questions basées sur le vrai formulaire Hasdrubal
- [x] Navigation télécommande (D-pad)
- [x] Notation par étoiles (1–5)
- [x] Option "skip" par question
- [x] Envoi API REST (Retrofit)
- [x] Écran remerciement avec fermeture automatique
- [x] Design aux couleurs Hasdrubal

### Phase 2 — À développer (Ce CDC)
- [ ] **F1** : Affichage IP LAN + WAN sur écran d'accueil
- [ ] **F2** : Boutons Start Now / Later / Not Now
- [ ] **F4** : Sauvegarde automatique de la progression
- [ ] **F4** : Détection et reprise de progression au relancement
- [ ] **F4** : Expiration de progression après 24h
- [ ] **F4** : File d'attente pour réponses non envoyées (offline retry)
- [ ] **F5** : Ajout IP LAN/WAN dans le payload API
- [ ] **F5** : Validation minimale (questions 1 et 20 obligatoires)

### Phase 3 — Améliorations futures (optionnel)
- [ ] Champ numéro de chambre (saisie numérique via D-pad)
- [ ] Support multilingue (FR / AR / DE / RU)
- [ ] Animation de transition entre questions
- [ ] Dashboard local de consultation des réponses
- [ ] Synchronisation automatique des réponses en attente

---

## 8. Glossaire

| Terme | Définition |
|-------|-----------|
| **MQTT** | Protocole de messagerie légère utilisé pour communiquer avec les boîtiers TV |
| **Broker Mosquitto** | Serveur MQTT central qui reçoit et distribue les messages |
| **BoitierSystemService** | Service Android existant qui écoute le broker et lance les apps |
| **D-pad** | Croix directionnelle de la télécommande (Haut/Bas/Gauche/Droite/OK) |
| **SharedPreferences** | Système de stockage local Android (clé-valeur) |
| **Payload** | Données JSON envoyées au serveur API |
| **LAN IP** | Adresse IP locale (réseau de l'hôtel) |
| **WAN IP** | Adresse IP publique (internet) |
| **Skip** | Passer une question (service non utilisé par le client) |
| **Retry queue** | File d'attente locale pour renvoyer les données si le réseau était indisponible |

---

*Document rédigé pour le projet HotelSurveyApp — Hasdrubal Thalassa & Spa*
