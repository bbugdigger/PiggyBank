package com.bugdigger.piggybank.data.api

import com.bugdigger.piggybank.ApiEndpoints
import com.bugdigger.piggybank.ServerConfig
import com.bugdigger.piggybank.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class PiggyBankApi(
    private val baseUrl: String = ServerConfig.getBaseUrl()
) {
    private var authToken: String? = null
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    fun getAuthToken(): String? = authToken
    
    fun isAuthenticated(): Boolean = authToken != null
    
    private suspend inline fun <reified T> safeApiCall(
        block: HttpClient.() -> HttpResponse
    ): ApiResult<T> {
        return try {
            val response = client.block()
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body())
            } else {
                val errorBody = try {
                    response.bodyAsText()
                } catch (e: Exception) {
                    response.status.description
                }
                ApiResult.Error(errorBody, response.status.value)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
    
    private fun HttpRequestBuilder.addAuth() {
        authToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }
    
    // ==================== Auth API ====================
    
    suspend fun register(request: RegisterRequest): ApiResult<AuthResponse> {
        return safeApiCall {
            post(baseUrl + ApiEndpoints.AUTH_REGISTER) {
                setBody(request)
            }
        }
    }
    
    suspend fun login(request: LoginRequest): ApiResult<AuthResponse> {
        return safeApiCall {
            post(baseUrl + ApiEndpoints.AUTH_LOGIN) {
                setBody(request)
            }
        }
    }
    
    suspend fun getCurrentUser(): ApiResult<UserResponse> {
        return safeApiCall {
            get(baseUrl + ApiEndpoints.AUTH_ME) {
                addAuth()
            }
        }
    }
    
    // ==================== Accounts API ====================
    
    suspend fun getAccounts(): ApiResult<List<AccountResponse>> {
        return safeApiCall {
            get(baseUrl + ApiEndpoints.ACCOUNTS) {
                addAuth()
            }
        }
    }
    
    suspend fun getAccountsTree(): ApiResult<List<AccountTreeNode>> {
        return safeApiCall {
            get(baseUrl + ApiEndpoints.ACCOUNTS_TREE) {
                addAuth()
            }
        }
    }
    
    suspend fun getAccount(id: String): ApiResult<AccountResponse> {
        return safeApiCall {
            get(baseUrl + "${ApiEndpoints.ACCOUNTS}/$id") {
                addAuth()
            }
        }
    }
    
    suspend fun getAccountBalance(id: String): ApiResult<AccountWithBalanceResponse> {
        return safeApiCall {
            get(baseUrl + "${ApiEndpoints.ACCOUNTS}/$id/balance") {
                addAuth()
            }
        }
    }
    
    suspend fun createAccount(request: CreateAccountRequest): ApiResult<AccountResponse> {
        return safeApiCall {
            post(baseUrl + ApiEndpoints.ACCOUNTS) {
                addAuth()
                setBody(request)
            }
        }
    }
    
    suspend fun updateAccount(id: String, request: UpdateAccountRequest): ApiResult<AccountResponse> {
        return safeApiCall {
            put(baseUrl + "${ApiEndpoints.ACCOUNTS}/$id") {
                addAuth()
                setBody(request)
            }
        }
    }
    
    suspend fun deleteAccount(id: String): ApiResult<Unit> {
        return safeApiCall {
            delete(baseUrl + "${ApiEndpoints.ACCOUNTS}/$id") {
                addAuth()
            }
        }
    }
    
    // ==================== Transactions API ====================
    
    suspend fun getTransactions(
        page: Int = 1,
        pageSize: Int = 50,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<TransactionListResponse> {
        return safeApiCall {
            get(baseUrl + ApiEndpoints.TRANSACTIONS) {
                addAuth()
                parameter("page", page)
                parameter("pageSize", pageSize)
                startDate?.let { parameter("startDate", it) }
                endDate?.let { parameter("endDate", it) }
            }
        }
    }
    
    suspend fun getTransaction(id: String): ApiResult<TransactionResponse> {
        return safeApiCall {
            get(baseUrl + "${ApiEndpoints.TRANSACTIONS}/$id") {
                addAuth()
            }
        }
    }
    
    suspend fun createTransaction(request: CreateTransactionRequest): ApiResult<TransactionResponse> {
        return safeApiCall {
            post(baseUrl + ApiEndpoints.TRANSACTIONS) {
                addAuth()
                setBody(request)
            }
        }
    }
    
    suspend fun updateTransaction(id: String, request: UpdateTransactionRequest): ApiResult<TransactionResponse> {
        return safeApiCall {
            put(baseUrl + "${ApiEndpoints.TRANSACTIONS}/$id") {
                addAuth()
                setBody(request)
            }
        }
    }
    
    suspend fun deleteTransaction(id: String): ApiResult<Unit> {
        return safeApiCall {
            delete(baseUrl + "${ApiEndpoints.TRANSACTIONS}/$id") {
                addAuth()
            }
        }
    }
    
    suspend fun getAccountRegister(
        accountId: String,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<AccountRegisterResponse> {
        return safeApiCall {
            get(baseUrl + "${ApiEndpoints.ACCOUNTS}/$accountId/transactions") {
                addAuth()
                startDate?.let { parameter("startDate", it) }
                endDate?.let { parameter("endDate", it) }
            }
        }
    }
    
    suspend fun voidTransaction(id: String, reason: String? = null): ApiResult<TransactionResponse> {
        return safeApiCall {
            post(baseUrl + "${ApiEndpoints.TRANSACTIONS}/$id/void") {
                addAuth()
                setBody(VoidTransactionRequest(reason))
            }
        }
    }
    
    suspend fun unvoidTransaction(id: String): ApiResult<TransactionResponse> {
        return safeApiCall {
            post(baseUrl + "${ApiEndpoints.TRANSACTIONS}/$id/unvoid") {
                addAuth()
            }
        }
    }
    
    suspend fun updateReconcileStatus(splitId: String, status: String): ApiResult<SplitResponse> {
        return safeApiCall {
            patch(baseUrl + "${ApiEndpoints.SPLITS}/$splitId/reconcile") {
                addAuth()
                setBody(UpdateReconcileStatusRequest(status))
            }
        }
    }
}
