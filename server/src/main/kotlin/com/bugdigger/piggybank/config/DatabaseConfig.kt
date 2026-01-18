package com.bugdigger.piggybank.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.bugdigger.piggybank.data.tables.*

object DatabaseConfig {
    
    private fun hikariConfig(): HikariConfig {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/piggybank"
        config.username = System.getenv("DATABASE_USER") ?: "myuser"
        config.password = System.getenv("DATABASE_PASSWORD") ?: "mysecretpassword"
        config.maximumPoolSize = 10
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return config
    }
    
    fun init() {
        val dataSource = HikariDataSource(hikariConfig())
        Database.connect(dataSource)
        
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Accounts,
                Transactions,
                Splits,
                ExchangeRates,
                RecurringTransactions,
                RecurringSplits
            )
        }
    }
}
