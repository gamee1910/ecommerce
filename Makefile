.PHONY: up down logs ps clean kafka-topics redis-cli

# ── Infra ────────────────────────────────────────────────────────────────────
COMPOSE=docker compose -f infra/docker/docker-compose.yml
up:
	cp -n .env.example .env 2>/dev/null || true
	$(COMPOSE) up -d
	@echo "Services running:"
	@echo "		PostgreSQL -> localhost:5432"
	@echo "		Redis	   -> localhost:6379"
	@echo "		Kafka	   -> localhost:9092"
	@echo "		Kafka UI   -> http://localhost:8090"

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f --tail=100

ps:
	$(COMPOSE) ps

clean:
	$(COMPOSE) down -v --remove-orphans
	@echo "All volumns removed."

# ── Kafka ────────────────────────────────────────────────────────────────────
kafka-topics:
	docker exec ecom-kafka kafka-topics --bootstrap-server localhost:9092 --list

kafka-create-topics:
	docker exec ecom-kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic order.created	--partitions 3 --relication-factor 1
	docker exec ecom-kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic order.created.DLT --partitions 3 --relication-factor 1
	docker exec ecom-kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic user.registered  --partitions 3 --replication-factor 1
	@echo "Topics created."

# ── Redis ────────────────────────────────────────────────────────────────────
redis-cli:
	docker exec -it ecom-redis redis-cli -a $${REDIS_PASSWORD:redis123}

# ── DB ───────────────────────────────────────────────────────────────────────
psql-users:
	docker exec -it ecom-postgres psql -U $${POSTGRES_USER:ecom} -d users_db

psql-products:
	docker exec -it ecom-postgres psql -U $${POSTGRES_USER:ecom} -d products_db

psql-orders:
	docker exec -it ecom-postgres psql -U $${POSTGRES_USER:ecom} -d orders_db

psql-notifications:
	docker exec -it ecom-postgres psql -U $${POSTGRES_USER:ecom} -d notifications_db

