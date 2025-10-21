# 📦 Système de Gestion de Commandes avec Kafka Streams

Un projet Spring Boot 3.1 complet illustrant l'utilisation avancée de **Apache Kafka** avec des **Producers**, **Consumers** et **Kafka Streams** pour le traitement en temps réel.

## 🎯 Objectifs d'Apprentissage

Ce projet démontre les concepts Kafka suivants:

### 1. **Producers (Producteurs)**
- ✅ Envoi asynchrone de messages avec callbacks
- ✅ Headers personnalisés pour métadonnées
- ✅ Partitionnement par clé (orderId)
- ✅ Idempotence et retry automatique
- ✅ Compression des messages (Snappy)
- ✅ Batching pour performance optimale

### 2. **Consumers (Consommateurs)**
- ✅ Multiples consumer groups
- ✅ Traitement parallèle avec concurrence
- ✅ Batch processing pour les événements
- ✅ Extraction des headers Kafka
- ✅ Gestion des offsets
- ✅ Error handling avec ErrorHandlingDeserializer

### 3. **Kafka Streams**
- ✅ Agrégations en temps réel
- ✅ Fenêtres temporelles (Time Windows)
- ✅ Comptage et statistiques
- ✅ Filtrage et transformation
- ✅ Détection de patterns
- ✅ Analyses avancées

## 🏗️ Architecture

```
┌─────────────┐
│   REST API  │ ──► order.created
└─────────────┘
                      │
        ┌─────────────┼─────────────┬──────────────┐
        │             │             │              │
        ▼             ▼             ▼              ▼
  OrderConsumer  Validation   Inventory      EventLog
                 Consumer     Consumer       Consumer
                      │             │
                      ▼             ▼
              order.validated  order.inventory
                      │             │
                      └──────┬──────┘
                             ▼
                     Payment Consumer
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
              order.shipped    order.notifications
                    │                 │
                    ▼                 ▼
              Shipping         Notification
              Consumer          Consumer

┌──────────────────────────────────────┐
│       Kafka Streams Analytics        │
├──────────────────────────────────────┤
│ • Comptage par statut (5 min)       │
│ • Détection commandes haute valeur  │
│ • Calcul revenu total (10 min)      │
│ • Commandes par client              │
│ • Produits populaires (15 min)      │
└──────────────────────────────────────┘
```

## 📊 Topics Kafka

| Topic | Partitions | Description |
|-------|-----------|-------------|
| `order.created` | 5 | Nouvelles commandes créées |
| `order.validated` | 5 | Commandes validées |
| `order.payment` | 3 | Paiements en cours |
| `order.shipped` | 3 | Commandes expédiées |
| `order.events` | 5 | Tous les événements de changement d'état |
| `order.notifications` | 3 | Notifications clients |
| `order.analytics` | 3 | Résultats d'analyse en temps réel |
| `order.inventory` | 5 | Gestion du stock |
| `order.dead-letter` | 1 | Messages en erreur |
| `order.retry` | 3 | Retry automatique |

## 🚀 Démarrage Rapide

### Prérequis
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Démarrer Kafka

```bash
docker-compose up -d
```

Cela démarre:
- 🐘 **Zookeeper** (port 2181)
- 🎯 **Kafka** (port 9092)
- 🖥️ **Kafka UI** (http://localhost:8090)

### 2. Compiler le projet

```bash
mvn clean install
```

### 3. Démarrer l'application

```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8080**

## 🧪 Tester le Système

### 1. Créer une commande normale

```bash
curl -X POST http://localhost:8080/api/orders/example | \
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @-
```

### 2. Créer une commande haute valeur (>1000€)

```bash
curl -X POST http://localhost:8080/api/orders/example/high-value | \
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @-
```

### 3. Créer une commande personnalisée

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerName": "Sophie Martin",
    "customerEmail": "sophie.martin@example.com",
    "phoneNumber": "+33 6 12 34 56 78",
    "priority": "EXPRESS",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "iPhone 15 Pro",
        "sku": "IPH15P-256-BLK",
        "quantity": 2,
        "unitPrice": 1299.99,
        "totalPrice": 2599.98,
        "category": "Smartphones",
        "weight": 0.3
      }
    ],
    "shippingAddress": {
      "street": "10 Rue de Rivoli",
      "city": "Paris",
      "state": "Île-de-France",
      "postalCode": "75001",
      "country": "France",
      "phoneNumber": "+33 6 12 34 56 78"
    },
    "billingAddress": {
      "street": "10 Rue de Rivoli",
      "city": "Paris",
      "state": "Île-de-France",
      "postalCode": "75001",
      "country": "France",
      "phoneNumber": "+33 6 12 34 56 78"
    },
    "paymentInfo": {
      "paymentMethod": "CREDIT_CARD",
      "cardLastFour": "5678",
      "paymentProcessor": "Stripe"
    },
    "notes": "Livraison express demandée"
  }'
```

### 4. Vérifier la santé

```bash
curl http://localhost:8080/api/orders/health
```

## 📈 Visualisation avec Kafka UI

Accédez à **http://localhost:8090** pour:

- 📋 Voir tous les topics et leurs messages
- 📊 Monitorer les consumer groups et leur lag
- 🔍 Rechercher et filtrer les messages
- 📈 Visualiser les métriques de performance
- ⚙️ Gérer les configurations Kafka

## 🔍 Concepts Kafka Détaillés

### Producers - Patterns Implémentés

#### 1. **Fire-and-Forget avec Callback**
```java
orderKafkaTemplate.send(record)
    .whenComplete((result, ex) -> {
        // Gestion asynchrone du résultat
    });
```

#### 2. **Headers Personnalisés**
```java
record.headers().add(new RecordHeader("priority", "EXPRESS".getBytes()));
record.headers().add(new RecordHeader("customer-id", customerId.getBytes()));
```

#### 3. **Idempotence**
```yaml
enable.idempotence: true
acks: all
retries: 3
```

### Consumers - Patterns Implémentés

#### 1. **Single Record Processing**
```java
@KafkaListener(topics = "order.created")
public void consume(Order order) {
    // Traitement message par message
}
```

#### 2. **Batch Processing**
```java
@KafkaListener(topics = "order.events")
public void consumeBatch(List<OrderEvent> events) {
    // Traitement par lot pour performance
}
```

#### 3. **Header Extraction**
```java
@KafkaListener(topics = "order.created")
public void consume(
    @Payload Order order,
    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
    @Header(KafkaHeaders.OFFSET) long offset
) {
    // Accès aux métadonnées Kafka
}
```

#### 4. **Multiple Consumer Groups**
- `order-processing-group` - Traitement principal
- `validation-group` - Validation des commandes
- `inventory-group` - Gestion du stock
- `payment-group` - Traitement des paiements
- `event-logging-group` - Log des événements
- `notification-group` - Envoi de notifications

### Kafka Streams - Analyses Implémentées

#### 1. **Comptage par Statut (Fenêtre de 5 min)**
```java
orderEventsStream
    .groupBy((key, event) -> event.getStatus())
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .count()
```

#### 2. **Détection Commandes Haute Valeur**
```java
orderStream
    .filter((key, order) -> order.getTotalAmount() > 1000)
    .peek((key, order) -> log.warn("HIGH VALUE ORDER"))
```

#### 3. **Calcul Revenu Total (Fenêtre de 10 min)**
```java
orderStream
    .groupBy((key, order) -> "TOTAL_REVENUE")
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(10)))
    .aggregate(() -> 0.0, (key, order, agg) -> agg + order.getTotal())
```

#### 4. **Produits Populaires (Fenêtre de 15 min)**
```java
orderStream
    .flatMapValues(order -> order.getItems())
    .groupBy((key, item) -> item.getProductId())
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(15)))
    .count()
```

## 🔄 Flux de Traitement Complet

1. **Création** → REST API reçoit commande
2. **Publication** → Producer envoie sur `order.created`
3. **Confirmation** → OrderConsumer confirme réception
4. **Validation** → ValidationConsumer valide les données
   - ✅ Email valide
   - ✅ Adresse complète
   - ✅ Montants corrects
   - ✅ Quantités positives
5. **Inventaire** → InventoryConsumer vérifie le stock
6. **Paiement** → PaymentConsumer traite le paiement
7. **Expédition** → Si succès, commande expédiée
8. **Notification** → NotificationConsumer envoie email
9. **Analytics** → Kafka Streams analyse en temps réel

## 📊 Monitoring et Métriques

### Actuator Endpoints

- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### Logs à Observer

```bash
# Démarrer avec logs détaillés
mvn spring-boot:run

# Observer les logs en temps réel
tail -f logs/application.log
```

Logs importants:
- 📤 Messages envoyés (partition, offset)
- 🔵 Messages reçus (consumer group, partition)
- ✅ Succès de traitement
- ❌ Erreurs et retry
- 📊 Analyses Streams
- 🚨 Alertes haute valeur

## ⚙️ Configuration Importante

### Producer Configuration
```yaml
acks: all                          # Attendre confirmation de tous les replicas
retries: 3                         # 3 tentatives en cas d'échec
enable.idempotence: true           # Éviter les duplications
compression.type: snappy           # Compression des messages
linger.ms: 10                      # Batching pour performance
batch.size: 32768                  # Taille des batches
```

### Consumer Configuration
```yaml
auto-offset-reset: earliest        # Lire depuis le début
enable-auto-commit: true           # Commit automatique des offsets
max-poll-records: 100              # Max messages par poll
fetch-min-bytes: 1024              # Attendre min 1KB
fetch-max-wait-ms: 500             # Attendre max 500ms
```

### Streams Configuration
```yaml
commit.interval.ms: 1000           # Commit toutes les secondes
num.stream.threads: 2              # 2 threads de traitement
cache.max.bytes.buffering: 0       # Pas de cache (temps réel)
```

## 🛠️ Fonctionnalités Avancées

### 1. Dead Letter Queue (DLQ)
Messages en erreur sont envoyés vers `order.dead-letter` pour investigation.

### 2. Retry Topic
Messages temporairement en échec sont envoyés vers `order.retry`.

### 3. Priority Routing
Messages sont routés selon leur priorité (EXPRESS, URGENT, etc.).

### 4. Async Processing
Traitement asynchrone des notifications avec `@Async`.

### 5. Idempotence
Évite les duplications avec idempotence activée côté producer.

## 📚 Ressources pour Apprendre

### Concepts Clés Démontrés

1. **Producers**
   - Synchrone vs Asynchrone
   - Callbacks et gestion d'erreurs
   - Partitionnement et routing
   - Compression et batching

2. **Consumers**
   - Consumer groups et parallélisme
   - Offset management
   - Batch vs single record
   - Error handling

3. **Kafka Streams**
   - KStream et KTable
   - Windowing (tumbling, hopping)
   - Aggregations et counting
   - Filtering et mapping
   - Joins et enrichments

## 🔧 Développement

### Structure du Projet

```
src/main/java/com/example/kafka/
├── config/              # Configurations Kafka
├── controller/          # REST API
├── consumer/            # Tous les consumers
├── producer/            # Tous les producers
├── streams/             # Kafka Streams processors
└── model/              # Domain models
```

### Ajouter un Nouveau Consumer

```java
@KafkaListener(
    topics = "mon-topic",
    groupId = "mon-group",
    containerFactory = "monContainerFactory"
)
public void consume(Order order) {
    // Votre logique ici
}
```

### Ajouter un Nouveau Stream

```java
@Autowired
public void buildMyStream(StreamsBuilder streamsBuilder) {
    KStream<String, String> stream = streamsBuilder
        .stream("input-topic");

    stream
        .filter(...)
        .mapValues(...)
        .to("output-topic");
}
```

## 🧹 Arrêt et Nettoyage

```bash
# Arrêter l'application
Ctrl+C

# Arrêter Kafka
docker-compose down

# Nettoyer les volumes (ATTENTION: supprime les données)
docker-compose down -v
```

## 🐛 Troubleshooting

### Kafka ne démarre pas
```bash
docker-compose logs kafka
# Vérifier les erreurs dans les logs
```

### Consumer ne reçoit pas les messages
- Vérifier le consumer group
- Vérifier l'offset (earliest vs latest)
- Vérifier les logs de désérialisation

### Streams ne traite pas
- Vérifier l'application-id
- Vérifier les state stores
- Vérifier les logs Kafka Streams

## 📝 Exercices Pratiques

1. **Ajouter un nouveau consumer** pour gérer les retours
2. **Créer un stream** pour détecter les clients fidèles (>5 commandes)
3. **Implémenter un DLQ** pour les messages en erreur
4. **Ajouter des métriques** personnalisées avec Micrometer
5. **Créer un consumer batch** pour optimiser les notifications

## 🎓 Conclusion

Ce projet couvre tous les aspects fondamentaux et avancés de Kafka:
- ✅ Producers avec différentes stratégies
- ✅ Consumers avec patterns variés
- ✅ Kafka Streams pour analytics temps réel
- ✅ Gestion d'erreurs et retry
- ✅ Monitoring et métriques
- ✅ Best practices de production

Parfait pour comprendre Kafka dans un contexte réel d'e-commerce! 🚀