# Migration to Clean Architecture: Feature-Package → Layered Clean Architecture

## Overview

This document outlines the complete migration strategy for refactoring a Java Spring Boot microservice from feature-based package organization to clean architecture (hexagonal/layered approach).

**Current State**: Feature-package structure
```
com.example.paymentservice
├── account
│   ├── AccountController
│   ├── AccountService
│   ├── AccountRepository
│   └── Account
├── transaction
│   ├── TransactionController
│   ├── TransactionService
│   ├── TransactionRepository
│   └── Transaction
└── notification
    ├── NotificationController
    ├── NotificationService
    └── Notification
```

**Target State**: Clean architecture with clear separation
```
com.example.paymentservice
├── application
│   ├── dto
│   ├── service
│   ├── mapper
│   └── exception
├── domain
│   ├── model
│   ├── repository
│   ├── service
│   └── event
├── infrastructure
│   ├── persistence
│   ├── external
│   ├── config
│   └── cache
├── presentation
│   ├── controller
│   ├── handler
│   └── validator
└── common
    ├── constant
    ├── util
    └── enums
```

---

## Phase 1: Infrastructure Setup

### 1.1 Create Core Package Structure

```
application/
├── dto/
│   ├── request/
│   │   ├── CreateAccountRequest.java
│   │   └── UpdateTransactionRequest.java
│   ├── response/
│   │   ├── AccountResponse.java
│   │   └── TransactionResponse.java
│   └── PageableResponse.java
├── service/
│   ├── AccountAppService.java
│   ├── TransactionAppService.java
│   └── NotificationAppService.java
├── mapper/
│   ├── AccountMapper.java
│   └── TransactionMapper.java
└── exception/
    └── ApplicationException.java

domain/
├── model/
│   ├── Account.java
│   ├── Transaction.java
│   └── Notification.java
├── repository/
│   ├── AccountRepository.java
│   ├── TransactionRepository.java
│   └── NotificationRepository.java
├── service/
│   └── DomainService.java
├── event/
│   ├── AccountCreatedEvent.java
│   └── TransactionProcessedEvent.java
└── exception/
    ├── AccountNotFoundException.java
    └── InvalidTransactionException.java

infrastructure/
├── persistence/
│   ├── jpa/
│   │   ├── entity/
│   │   │   ├── AccountEntity.java
│   │   │   └── TransactionEntity.java
│   │   ├── repository/
│   │   │   ├── AccountJpaRepository.java
│   │   │   └── AccountRepositoryImpl.java
│   │   └── mapper/
│   │       └── AccountPersistenceMapper.java
│   └── config/
│       └── PersistenceConfig.java
├── external/
│   ├── payment/
│   │   ├── PaymentGatewayClient.java
│   │   └── PaymentGatewayAdapter.java
│   └── notification/
│       ├── EmailService.java
│       └── SmsService.java
├── cache/
│   ├── AccountCacheManager.java
│   └── CacheConfig.java
├── config/
│   ├── DatabaseConfig.java
│   └── RedisConfig.java
└── event/
    └── EventPublisherAdapter.java

presentation/
├── controller/
│   ├── AccountController.java
│   ├── TransactionController.java
│   └── HealthController.java
├── handler/
│   └── GlobalExceptionHandler.java
└── validator/
    ├── AccountValidator.java
    └── TransactionValidator.java

common/
├── constant/
│   ├── ApiEndpoints.java
│   ├── ErrorCodes.java
│   └── CacheKeys.java
├── util/
│   ├── DateTimeUtil.java
│   └── CryptUtil.java
└── enums/
    ├── TransactionStatus.java
    └── AccountType.java
```

---

## Phase 2: Domain Layer - Core Business Logic

### 2.1 Domain Models

**Account.java** - Aggregate Root
```java
public class Account {
    private final String id;
    private final String customerId;
    private final String accountNumber;
    private AccountStatus status;
    private BigDecimal balance;
    private final LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    public Account(String id, String customerId, String accountNumber,
                   AccountStatus status, BigDecimal balance,
                   LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active");
        }
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new AccountAlreadyClosedException("Account already closed");
        }
        this.status = AccountStatus.CLOSED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public AccountStatus getStatus() { return status; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
}
```

**Transaction.java** - Entity
```java
public class Transaction {
    private final String id;
    private final String sourceAccountId;
    private final String destinationAccountId;
    private final BigDecimal amount;
    private final String description;
    private TransactionStatus status;
    private final TransactionType type;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public Transaction(String id, String sourceAccountId, String destinationAccountId,
                      BigDecimal amount, String description, TransactionStatus status,
                      TransactionType type, LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.type = type;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public void markAsCompleted() {
        if (status == TransactionStatus.COMPLETED) {
            throw new TransactionAlreadyCompletedException("Already completed");
        }
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        if (status == TransactionStatus.COMPLETED) {
            throw new CannotFailCompletedTransactionException("Cannot fail completed transaction");
        }
        this.status = TransactionStatus.FAILED;
    }

    public String getId() { return id; }
    public String getSourceAccountId() { return sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public TransactionStatus getStatus() { return status; }
    public TransactionType getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
```

### 2.2 Domain Repository Interfaces

**AccountRepository.java**
```java
public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(String id);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByCustomerId(String customerId);
    void delete(String id);
}
```

**TransactionRepository.java**
```java
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(String id);
    List<Transaction> findBySourceAccountId(String accountId);
    List<Transaction> findByDestinationAccountId(String accountId);
    List<Transaction> findByAccountIdAndDateRange(String accountId, LocalDateTime from, LocalDateTime to);
}
```

### 2.3 Domain Services

**AccountDomainService.java**
```java
public class AccountDomainService {
    private final AccountRepository accountRepository;

    public AccountDomainService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(String customerId, String accountNumber,
                                  AccountStatus status, BigDecimal initialBalance) {
        Account account = new Account(
            UUID.randomUUID().toString(),
            customerId,
            accountNumber,
            status,
            initialBalance,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        return accountRepository.save(account);
    }

    public void transferBetweenAccounts(String fromAccountId, String toAccountId,
                                        BigDecimal amount) {
        Account sourceAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new AccountNotFoundException("Source account not found"));
        Account destinationAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));

        sourceAccount.debit(amount);
        destinationAccount.credit(amount);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
    }
}
```

**TransactionDomainService.java**
```java
public class TransactionDomainService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionDomainService(TransactionRepository transactionRepository,
                                    AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public Transaction initiateTransaction(String sourceAccountId, String destinationAccountId,
                                           BigDecimal amount, String description,
                                           TransactionType type) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
            .orElseThrow(() -> new AccountNotFoundException("Source account not found"));

        sourceAccount.debit(amount);
        accountRepository.save(sourceAccount);

        Transaction transaction = new Transaction(
            UUID.randomUUID().toString(),
            sourceAccountId,
            destinationAccountId,
            amount,
            description,
            TransactionStatus.PENDING,
            type,
            LocalDateTime.now(),
            null
        );

        return transactionRepository.save(transaction);
    }

    public void completeTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        Account destinationAccount = accountRepository.findById(transaction.getDestinationAccountId())
            .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));

        destinationAccount.credit(transaction.getAmount());
        accountRepository.save(destinationAccount);

        transaction.markAsCompleted();
        transactionRepository.save(transaction);
    }

    public void failTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        Account sourceAccount = accountRepository.findById(transaction.getSourceAccountId())
            .orElseThrow(() -> new AccountNotFoundException("Source account not found"));

        sourceAccount.credit(transaction.getAmount());
        accountRepository.save(sourceAccount);

        transaction.markAsFailed();
        transactionRepository.save(transaction);
    }
}
```

### 2.4 Domain Events

**AccountCreatedEvent.java**
```java
public class AccountCreatedEvent {
    private final String accountId;
    private final String customerId;
    private final String accountNumber;
    private final BigDecimal initialBalance;
    private final LocalDateTime occurredAt;

    public AccountCreatedEvent(String accountId, String customerId, String accountNumber,
                               BigDecimal initialBalance, LocalDateTime occurredAt) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.initialBalance = initialBalance;
        this.occurredAt = occurredAt;
    }

    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
```

**TransactionCompletedEvent.java**
```java
public class TransactionCompletedEvent {
    private final String transactionId;
    private final String sourceAccountId;
    private final String destinationAccountId;
    private final BigDecimal amount;
    private final LocalDateTime occurredAt;

    public TransactionCompletedEvent(String transactionId, String sourceAccountId,
                                     String destinationAccountId, BigDecimal amount,
                                     LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public String getTransactionId() { return transactionId; }
    public String getSourceAccountId() { return sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
```

---

## Phase 3: Application Layer - Use Cases & Orchestration

### 3.1 Application Services

**AccountAppService.java**
```java
@Service
@Transactional
public class AccountAppService {
    private final AccountRepository accountRepository;
    private final AccountDomainService accountDomainService;
    private final AccountMapper accountMapper;
    private final EventPublisher eventPublisher;
    private final AccountCacheManager cacheManager;

    public AccountAppService(AccountRepository accountRepository,
                             AccountDomainService accountDomainService,
                             AccountMapper accountMapper,
                             EventPublisher eventPublisher,
                             AccountCacheManager cacheManager) {
        this.accountRepository = accountRepository;
        this.accountDomainService = accountDomainService;
        this.accountMapper = accountMapper;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = accountDomainService.createAccount(
            request.getCustomerId(),
            request.getAccountNumber(),
            AccountStatus.ACTIVE,
            request.getInitialBalance()
        );

        AccountCreatedEvent event = new AccountCreatedEvent(
            account.getId(),
            account.getCustomerId(),
            account.getAccountNumber(),
            account.getBalance(),
            LocalDateTime.now()
        );
        eventPublisher.publish(event);

        cacheManager.evictCustomerAccounts(request.getCustomerId());

        return accountMapper.toResponse(account);
    }

    public AccountResponse getAccount(String accountId) {
        Account account = cacheManager.getAccount(accountId);
        if (account == null) {
            account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
            cacheManager.cacheAccount(account);
        }
        return accountMapper.toResponse(account);
    }

    public List<AccountResponse> getCustomerAccounts(String customerId) {
        List<Account> accounts = cacheManager.getCustomerAccounts(customerId);
        if (accounts == null) {
            accounts = accountRepository.findByCustomerId(customerId);
            cacheManager.cacheCustomerAccounts(customerId, accounts);
        }
        return accounts.stream()
            .map(accountMapper::toResponse)
            .collect(Collectors.toList());
    }

    public AccountResponse closeAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        account.close();
        Account savedAccount = accountRepository.save(account);

        cacheManager.evictAccount(accountId);
        cacheManager.evictCustomerAccounts(account.getCustomerId());

        return accountMapper.toResponse(savedAccount);
    }
}
```

**TransactionAppService.java**
```java
@Service
@Transactional
public class TransactionAppService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionDomainService transactionDomainService;
    private final TransactionMapper transactionMapper;
    private final EventPublisher eventPublisher;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryTemplate retryTemplate;

    public TransactionAppService(TransactionRepository transactionRepository,
                                 AccountRepository accountRepository,
                                 TransactionDomainService transactionDomainService,
                                 TransactionMapper transactionMapper,
                                 EventPublisher eventPublisher,
                                 CircuitBreakerRegistry circuitBreakerRegistry,
                                 RetryTemplate retryTemplate) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionDomainService = transactionDomainService;
        this.transactionMapper = transactionMapper;
        this.eventPublisher = eventPublisher;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryTemplate = retryTemplate;
    }

    public TransactionResponse initiateTransfer(CreateTransactionRequest request) {
        Transaction transaction = transactionDomainService.initiateTransaction(
            request.getSourceAccountId(),
            request.getDestinationAccountId(),
            request.getAmount(),
            request.getDescription(),
            TransactionType.TRANSFER
        );

        try {
            retryTemplate.execute(context ->
                processTransactionWithCircuitBreaker(transaction.getId())
            );
        } catch (Exception e) {
            transactionDomainService.failTransaction(transaction.getId());
            throw new TransactionProcessingException("Failed to process transaction", e);
        }

        Transaction completedTransaction = transactionRepository.findById(transaction.getId())
            .orElseThrow();

        publishTransactionEvent(completedTransaction);

        return transactionMapper.toResponse(completedTransaction);
    }

    private Void processTransactionWithCircuitBreaker(String transactionId) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("transaction-processor");
        return circuitBreaker.executeSupplier(() -> {
            transactionDomainService.completeTransaction(transactionId);
            return null;
        });
    }

    private void publishTransactionEvent(Transaction transaction) {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
            transaction.getId(),
            transaction.getSourceAccountId(),
            transaction.getDestinationAccountId(),
            transaction.getAmount(),
            LocalDateTime.now()
        );
        eventPublisher.publish(event);
    }

    public List<TransactionResponse> getAccountTransactions(String accountId, int page, int size) {
        List<Transaction> transactions = transactionRepository.findBySourceAccountId(accountId);
        return transactions.stream()
            .map(transactionMapper::toResponse)
            .collect(Collectors.toList());
    }

    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        return transactionMapper.toResponse(transaction);
    }
}
```

### 3.2 DTOs & Mappers

**CreateAccountRequest.java**
```java
public class CreateAccountRequest {
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.01", message = "Balance must be positive")
    private BigDecimal initialBalance;

    public CreateAccountRequest() {}

    public CreateAccountRequest(String customerId, String accountNumber, BigDecimal initialBalance) {
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.initialBalance = initialBalance;
    }

    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getInitialBalance() { return initialBalance; }
}
```

**AccountResponse.java**
```java
public class AccountResponse {
    private String id;
    private String customerId;
    private String accountNumber;
    private String status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    public AccountResponse(String id, String customerId, String accountNumber,
                          String status, BigDecimal balance,
                          LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public String getStatus() { return status; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
}
```

**AccountMapper.java**
```java
@Component
public class AccountMapper {
    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getCustomerId(),
            account.getAccountNumber(),
            account.getStatus().name(),
            account.getBalance(),
            account.getCreatedAt(),
            account.getLastModifiedAt()
        );
    }
}
```

### 3.3 Exception Handling

**ApplicationException.java**
```java
public abstract class ApplicationException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public ApplicationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ApplicationException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
```

**AccountNotFoundException.java**
```java
public class AccountNotFoundException extends ApplicationException {
    public AccountNotFoundException(String message) {
        super(message, "ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
```

**InsufficientFundsException.java**
```java
public class InsufficientFundsException extends ApplicationException {
    public InsufficientFundsException(String message) {
        super(message, "INSUFFICIENT_FUNDS", HttpStatus.BAD_REQUEST);
    }
}
```

---

## Phase 4: Infrastructure Layer - Persistence & Adapters

### 4.1 JPA Entities & Repositories

**AccountEntity.java** - JPA Entity
```java
@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    private Long version;

    public AccountEntity() {}

    public AccountEntity(String id, String customerId, String accountNumber,
                        AccountStatus status, BigDecimal balance,
                        LocalDateTime createdAt, LocalDateTime lastModifiedAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public AccountStatus getStatus() { return status; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public Long getVersion() { return version; }

    public void setStatus(AccountStatus status) { this.status = status; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
}
```

**AccountJpaRepository.java** - Spring Data Repository
```java
public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
    List<AccountEntity> findByCustomerId(String customerId);
    List<AccountEntity> findByStatus(AccountStatus status);
}
```

**AccountRepositoryImpl.java** - Repository Implementation
```java
@Repository
public class AccountRepositoryImpl implements AccountRepository {
    private final AccountJpaRepository jpaRepository;
    private final AccountPersistenceMapper mapper;

    public AccountRepositoryImpl(AccountJpaRepository jpaRepository,
                                AccountPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        entity.setLastModifiedAt(LocalDateTime.now());
        AccountEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Account> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber).map(mapper::toDomain);
    }

    @Override
    public List<Account> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerId(customerId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
}
```

**AccountPersistenceMapper.java** - Entity ↔ Domain Mapping
```java
@Component
public class AccountPersistenceMapper {
    public Account toDomain(AccountEntity entity) {
        return new Account(
            entity.getId(),
            entity.getCustomerId(),
            entity.getAccountNumber(),
            entity.getStatus(),
            entity.getBalance(),
            entity.getCreatedAt(),
            entity.getLastModifiedAt()
        );
    }

    public AccountEntity toEntity(Account domain) {
        return new AccountEntity(
            domain.getId(),
            domain.getCustomerId(),
            domain.getAccountNumber(),
            domain.getStatus(),
            domain.getBalance(),
            domain.getCreatedAt(),
            domain.getLastModifiedAt()
        );
    }
}
```

### 4.2 Caching Configuration

**AccountCacheManager.java**
```java
@Component
public class AccountCacheManager {
    private static final String ACCOUNT_CACHE_KEY = "account:";
    private static final String CUSTOMER_ACCOUNTS_CACHE_KEY = "customer-accounts:";
    private static final long ACCOUNT_TTL_MINUTES = 10;
    private static final long CUSTOMER_ACCOUNTS_TTL_MINUTES = 5;

    private final RedisTemplate<String, Account> redisTemplate;
    private final ConcurrentHashMap<String, Cache<Account>> localCache;

    public AccountCacheManager(RedisTemplate<String, Account> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.localCache = new ConcurrentHashMap<>();
    }

    public void cacheAccount(Account account) {
        String key = ACCOUNT_CACHE_KEY + account.getId();
        redisTemplate.opsForValue().set(key, account,
            Duration.ofMinutes(ACCOUNT_TTL_MINUTES));
    }

    public Account getAccount(String accountId) {
        String key = ACCOUNT_CACHE_KEY + accountId;
        return redisTemplate.opsForValue().get(key);
    }

    public void evictAccount(String accountId) {
        String key = ACCOUNT_CACHE_KEY + accountId;
        redisTemplate.delete(key);
    }

    public void cacheCustomerAccounts(String customerId, List<Account> accounts) {
        String key = CUSTOMER_ACCOUNTS_CACHE_KEY + customerId;
        redisTemplate.opsForValue().set(key, accounts,
            Duration.ofMinutes(CUSTOMER_ACCOUNTS_TTL_MINUTES));
    }

    @SuppressWarnings("unchecked")
    public List<Account> getCustomerAccounts(String customerId) {
        String key = CUSTOMER_ACCOUNTS_CACHE_KEY + customerId;
        Object cached = redisTemplate.opsForValue().get(key);
        return (cached instanceof List) ? (List<Account>) cached : null;
    }

    public void evictCustomerAccounts(String customerId) {
        String key = CUSTOMER_ACCOUNTS_CACHE_KEY + customerId;
        redisTemplate.delete(key);
    }
}
```

### 4.3 Event Publishing Adapter

**EventPublisherAdapter.java** - Kafka Event Publisher
```java
@Component
public class EventPublisherAdapter {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(AccountCreatedEvent event) {
        publishEvent("account-created", event.getAccountId(), event);
    }

    public void publish(TransactionCompletedEvent event) {
        publishEvent("transaction-completed", event.getTransactionId(), event);
    }

    private <T> void publishEvent(String topic, String key, T event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload)
                .addCallback(
                    result -> {},
                    ex -> {
                        throw new EventPublishingException("Failed to publish event", ex);
                    }
                );
        } catch (JsonProcessingException e) {
            throw new EventPublishingException("Failed to serialize event", e);
        }
    }
}
```

### 4.4 Circuit Breaker Configuration

**CircuitBreakerConfig.java**
```java
@Configuration
public class CircuitBreakerConfig {
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreaker transactionProcessorCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .slowCallRateThreshold(50.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .minimumNumberOfCalls(5)
            .slidingWindowSize(10)
            .recordExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(BusinessLogicException.class)
            .build();

        return registry.circuitBreaker("transaction-processor", config);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L);
        template.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }
}
```

**TimeoutConfig.java**
```java
@Configuration
public class TimeoutConfig {
    @Bean
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }

    @Bean
    public TimeLimiter transactionTimeLimiter(TimeLimiterRegistry registry) {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .cancelRunningFuture(true)
            .build();

        return registry.timeLimiter("transaction-processor", config);
    }
}
```

### 4.5 External Service Adapters

**PaymentGatewayAdapter.java** - External API Adapter
```java
@Component
public class PaymentGatewayAdapter {
    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    public PaymentGatewayAdapter(RestTemplate restTemplate,
                                 CircuitBreakerRegistry circuitBreakerRegistry,
                                 TimeLimiterRegistry timeLimiterRegistry) {
        this.restTemplate = restTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    public PaymentResult processPayment(String accountId, BigDecimal amount, String reference) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-gateway");
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("payment-gateway");

        return circuitBreaker.executeSupplier(() ->
            timeLimiter.executeFutureSupplier(() ->
                CompletableFuture.supplyAsync(() -> callPaymentGateway(accountId, amount, reference))
            ).get()
        );
    }

    private PaymentResult callPaymentGateway(String accountId, BigDecimal amount, String reference) {
        try {
            String url = "https://payment-gateway.example.com/api/v1/process";
            PaymentRequest request = new PaymentRequest(accountId, amount, reference);
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url, request, PaymentResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return PaymentResult.success(response.getBody().getTransactionId());
            } else {
                return PaymentResult.failure("Payment gateway returned error");
            }
        } catch (RestClientException e) {
            throw new ExternalServiceException("Payment gateway service unavailable", e);
        }
    }
}
```

---

## Phase 5: Presentation Layer - Controllers & Handlers

### 5.1 REST Controllers

**AccountController.java**
```java
@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class AccountController {
    private final AccountAppService accountAppService;
    private final AccountValidator validator;

    public AccountController(AccountAppService accountAppService,
                             AccountValidator validator) {
        this.accountAppService = accountAppService;
        this.validator = validator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        validator.validateCreateAccountRequest(request);
        return accountAppService.createAccount(request);
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable String accountId) {
        return accountAppService.getAccount(accountId);
    }

    @GetMapping("/customer/{customerId}")
    public List<AccountResponse> getCustomerAccounts(@PathVariable String customerId) {
        return accountAppService.getCustomerAccounts(customerId);
    }

    @PutMapping("/{accountId}/close")
    public AccountResponse closeAccount(@PathVariable String accountId) {
        return accountAppService.closeAccount(accountId);
    }
}
```

**TransactionController.java**
```java
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {
    private final TransactionAppService transactionAppService;
    private final TransactionValidator validator;

    public TransactionController(TransactionAppService transactionAppService,
                                  TransactionValidator validator) {
        this.transactionAppService = transactionAppService;
        this.validator = validator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionResponse initiateTransfer(@Valid @RequestBody CreateTransactionRequest request) {
        validator.validateCreateTransactionRequest(request);
        return transactionAppService.initiateTransfer(request);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getTransaction(@PathVariable String transactionId) {
        return transactionAppService.getTransaction(transactionId);
    }

    @GetMapping("/account/{accountId}")
    public List<TransactionResponse> getAccountTransactions(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionAppService.getAccountTransactions(accountId, page, size);
    }
}
```

### 5.2 Global Exception Handler

**GlobalExceptionHandler.java**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String CODE = "code";
    private static final String MESSAGE = "message";
    private static final String PATH = "path";

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationException(
            ApplicationException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
            ex.getHttpStatus(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            message,
            request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getRequestURI()
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, status.value());
        body.put(ERROR, status.getReasonPhrase());
        body.put(CODE, code);
        body.put(MESSAGE, message);
        body.put(PATH, path);

        return new ResponseEntity<>(body, status);
    }
}
```

### 5.3 Input Validators

**AccountValidator.java**
```java
@Component
public class AccountValidator {
    private final AccountRepository accountRepository;

    public AccountValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void validateCreateAccountRequest(CreateAccountRequest request) {
        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Initial balance must be positive");
        }

        accountRepository.findByAccountNumber(request.getAccountNumber())
            .ifPresent(account -> {
                throw new ValidationException("Account number already exists");
            });
    }
}
```

---

## Phase 6: Common Layer - Constants & Utilities

### 6.1 Constants

**ApiEndpoints.java**
```java
public final class ApiEndpoints {
    private ApiEndpoints() {
    }

    public static final String API_V1 = "/api/v1";
    public static final String ACCOUNTS = API_V1 + "/accounts";
    public static final String TRANSACTIONS = API_V1 + "/transactions";
    public static final String HEALTH = "/health";

    public static final String ACCOUNT_BY_ID = "/{accountId}";
    public static final String ACCOUNT_BY_CUSTOMER = "/customer/{customerId}";
    public static final String TRANSACTION_BY_ID = "/{transactionId}";
}
```

**ErrorCodes.java**
```java
public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String ACCOUNT_ALREADY_CLOSED = "ACCOUNT_ALREADY_CLOSED";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String INVALID_AMOUNT = "INVALID_AMOUNT";
    public static final String TRANSACTION_NOT_FOUND = "TRANSACTION_NOT_FOUND";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
}
```

**CacheKeys.java**
```java
public final class CacheKeys {
    private CacheKeys() {
    }

    public static final String ACCOUNT_PREFIX = "account:";
    public static final String CUSTOMER_ACCOUNTS_PREFIX = "customer-accounts:";
    public static final String TRANSACTION_PREFIX = "transaction:";
    public static final int ACCOUNT_TTL_MINUTES = 10;
    public static final int CUSTOMER_ACCOUNTS_TTL_MINUTES = 5;
}
```

### 6.2 Enums

**TransactionStatus.java**
```java
public enum TransactionStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**AccountStatus.java**
```java
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    CLOSED("Closed"),
    SUSPENDED("Suspended");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**TransactionType.java**
```java
public enum TransactionType {
    TRANSFER("Transfer"),
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    REVERSAL("Reversal");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### 6.3 Custom Exceptions

**ValidationException.java**
```java
public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super(message, ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
    }
}
```

**ExternalServiceException.java**
```java
public class ExternalServiceException extends ApplicationException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, "EXTERNAL_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
```

**EventPublishingException.java**
```java
public class EventPublishingException extends ApplicationException {
    public EventPublishingException(String message, Throwable cause) {
        super(message, "EVENT_PUBLISHING_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
```

---

## Phase 7: Configuration & Main Application

### 7.1 Spring Boot Main Class

**PaymentServiceApplication.java**
```java
@SpringBootApplication(scanBasePackages = {
    "com.example.paymentservice.application",
    "com.example.paymentservice.domain",
    "com.example.paymentservice.infrastructure",
    "com.example.paymentservice.presentation",
    "com.example.paymentservice.common"
})
@EnableConfigurationProperties
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
```

### 7.2 Application Configuration

**ApplicationConfig.java**
```java
@Configuration
public class ApplicationConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

### 7.3 Application Properties

**application.yml**
```yaml
spring:
  application:
    name: payment-service
  profiles:
    active: dev

  datasource:
    url: jdbc:postgresql://localhost:5432/payment_db
    username: postgres
    password: password
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true

  redis:
    host: localhost
    port: 6379
    timeout: 60000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3

server:
  port: 8080
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.example.paymentservice: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

---

## Migration Strategy

### Step 1: Setup New Package Structure
- Create all new packages in parallel with existing code
- No changes to existing code yet

### Step 2: Implement Domain Layer
- Create domain models with business logic
- Domain repositories (interfaces only)
- Domain services with core use cases
- Domain events

### Step 3: Implement Infrastructure
- Create entity classes
- Implement repository adapters
- Create external service adapters
- Setup caching

### Step 4: Implement Application Layer
- Application services orchestrating domain services
- DTOs and mappers
- Exception handling
- Start injecting infrastructure repositories into app services

### Step 5: Implement Presentation Layer
- Controllers using application services
- Global exception handler
- Input validators

### Step 6: Migrate Feature by Feature
- Redirect traffic from old controller to new controller (parallel running)
- Validate behavior matches
- Remove old code
- Follow order: Account → Transaction → Notification

### Step 7: Testing & Validation
- Run full integration tests
- Load testing for performance validation
- Cache behavior testing
- Circuit breaker/resilience testing
- Event publishing validation

### Step 8: Cleanup
- Remove old feature packages
- Remove old service/repository classes
- Update documentation
- Archive old code branch

---

## Key Design Principles Applied

### Separation of Concerns
Each layer has single responsibility: Domain handles business logic, Application orchestrates, Infrastructure handles persistence and external calls, Presentation handles requests.

### Dependency Inversion
Domain layer depends only on abstractions (repository interfaces). Higher layers depend on lower layers through injection.

### Resilience Patterns
Circuit breaker protects external service calls. Retry logic handles transient failures. Timeouts prevent hanging requests. Bulkheads isolate thread pools.

### Caching Strategy
L1: In-memory for frequently accessed data. L2: Redis for distributed caching. TTLs based on data change frequency.

### Event-Driven
Important domain events published to Kafka for async processing by other services.

### Testability
Dependencies injected. Business logic in domain models (easily testable). Application services testable with mocked repositories.

---

## pom.xml Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-kafka</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.5.0</version>
    </dependency>

    <!-- Resilience4j -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
        <version>2.0.2</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-circuitbreaker</artifactId>
        <version>2.0.2</version>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Testing Strategy

### Unit Tests: Domain Layer
```java
public class AccountTest {
    private Account account;

    @Before
    public void setup() {
        account = new Account("ACC-1", "CUST-1", "1234567890",
            AccountStatus.ACTIVE, new BigDecimal("1000.00"),
            LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    public void testDebitReducesBalance() {
        account.debit(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("900.00"), account.getBalance());
    }

    @Test
    public void testDebitInsufficientFundsFails() {
        assertThrows(InsufficientFundsException.class, () ->
            account.debit(new BigDecimal("2000.00")));
    }

    @Test
    public void testCreditIncreasesBalance() {
        account.credit(new BigDecimal("500.00"));
        assertEquals(new BigDecimal("1500.00"), account.getBalance());
    }
}
```

### Integration Tests: Application Layer
```java
@SpringBootTest
public class AccountAppServiceIntegrationTest {
    @Autowired
    private AccountAppService accountAppService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @Transactional
    public void testCreateAccountAndRetrieve() {
        CreateAccountRequest request = new CreateAccountRequest(
            "CUST-1", "ACC-1", new BigDecimal("1000.00"));

        AccountResponse response = accountAppService.createAccount(request);

        assertNotNull(response.getId());
        assertEquals("CUST-1", response.getCustomerId());

        AccountResponse retrieved = accountAppService.getAccount(response.getId());
        assertEquals(response.getId(), retrieved.getId());
    }
}
```

---

## Monitoring & Observability

**Metrics to Track:**
- Request latency (p50, p95, p99)
- Circuit breaker state changes
- Cache hit/miss rates
- Database query performance
- Event publishing success/failure rates
- External service call latencies

**Health Checks:**
- Database connectivity
- Redis connectivity
- Kafka broker availability
- External service availability

---

## Performance Considerations

### Database
- Index account_id, customer_id
- Use connection pooling (HikariCP with 10 max connections)
- Batch insert/update operations
- Query optimization with fetch_size=50

### Caching
- Cache accounts for 10 minutes
- Cache customer accounts list for 5 minutes
- Invalidate on write operations
- Use Redis for distributed caching

### API
- Implement rate limiting per customer
- Request timeout: 5 seconds default
- Response pagination for list endpoints
- API versioning support (v1 ready for v2)

---

## Deployment Checklist

- [ ] New package structure created
- [ ] All domain models implemented with business logic
- [ ] Repository interfaces and implementations
- [ ] Application services with orchestration
- [ ] Infrastructure configuration (DB, Redis, Kafka)
- [ ] Controllers and exception handlers
- [ ] All unit and integration tests passing
- [ ] Load testing completed
- [ ] Circuit breaker/resilience patterns validated
- [ ] Event publishing working end-to-end
- [ ] Cache invalidation strategy verified
- [ ] Documentation updated
- [ ] Parallel deployment with feature flags
- [ ] Monitoring and alerting configured
- [ ] Rollback plan documented
