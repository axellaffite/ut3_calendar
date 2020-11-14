package com.edt.ut3.backend.requests.authentication_services

import android.util.Log
import com.edt.ut3.backend.requests.CookieProvider
import com.edt.ut3.backend.requests.HttpClientProvider
import com.edt.ut3.misc.extensions.isNotNull
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeoutException

class AuthenticatorUT3: Authenticator() {

    @Throws(IOException::class, TimeoutException::class, InvalidCredentialsException::class)
    override suspend fun ensureAuthentication(host: String, provider: CookieProvider, credentials: Credentials) {
        Log.d(this::class.simpleName, "Checking authentication..")
        // Here we look for authentication cookies
        // If there are no cookie available for authentication
        // the resulting cookie is null.
        val cookies = CookieProvider.getCookiesFor(host)?.filter {
            it.name.matches(".*AspNetCore.Cookies.*".toRegex())
        }


        // The authentication is considered successful
        // if there is a cookie that is persistent
        val authenticated = containsValidAuthenticationCookie(cookies ?: listOf())


        // If we're not logged in,
        // we try to authenticate
        Log.d(this::class.simpleName, "Already authenticated: $authenticated")
        if (!authenticated) {
            authenticate(host, provider, credentials)
        }
    }

    @Throws(IOException::class, TimeoutException::class, InvalidCredentialsException::class)
    override suspend fun authenticate(host: String, provider: CookieProvider, credentials: Credentials) = withContext(IO) {
        Log.d(this@AuthenticatorUT3::class.simpleName, "Trying authentication to https://$host")


        // Building everything to start
        // the authentication.
        val retrofit = Retrofit.Builder()
            .baseUrl("https://$host")
            .client(HttpClientProvider.generateNewClient())
            .build()
        val service = retrofit.create(AuthUT3::class.java)

        // Here we launch the request
        // and extract the token from the response
        val response = service.getToken().execute().body()
        val nullableToken = getTokenFromResponse(response)

        // As the token can be not present
        // in the response, we must ensure that
        // it as been found.
        // Otherwise, the authentication cannot
        // be proceed as Celcat authentication
        // requires an anti-CSRF token.
        val authenticationSuccessful = nullableToken?.let { token ->
            // We launch the authentication
            // The request doesn't return cookies as they
            // are handled by the CookieProvider.
            service.auth(
                name = credentials.username,
                password = credentials.password,
                token = token
            ).execute()

            // We extract them from the cookie provider
            // and test if they are valid authentication cookies
            val cookies = provider.getCookiesFor(host)
            println(cookies)
            cookies?.let {
                containsValidAuthenticationCookie(it.toList())
            } ?: false
        } ?: false

        Log.d(this@AuthenticatorUT3::class.simpleName, "Authentication successful: $authenticationSuccessful")
        if (!authenticationSuccessful) {
            throw InvalidCredentialsException()
        }
    }

    @Throws(IOException::class)
    private fun getTokenFromResponse(body: ResponseBody?) = body?.run {
        val reg = Regex("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"(.*)\" />")

        reg.find(body.string())?.groups?.get(1)?.value
    }

    private fun containsValidAuthenticationCookie(cookies: List<Cookie>) : Boolean {
        cookies.forEach { println("${it.persistent}, ${it.expiresAt}") }
        return cookies.isNotNull() &&
                cookies.find { it.name.matches(Regex(".AspNetCore.Cookies")) }.isNotNull()
    }


    interface AuthUT3 {
        @GET("/calendar2/LdapLogin")
        fun getToken(): retrofit2.Call<ResponseBody>

        @FormUrlEncoded
        @POST("/calendar2/LdapLogin/Logon")
        fun auth(
            @Field("Name") name: String,
            @Field("Password") password: String,
            @Field("__RequestVerificationToken") token: String
        ): retrofit2.Call<ResponseBody>
    }

}