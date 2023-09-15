package com.edt.ut3.misc

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import io.ktor.util.KtorDsl
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RedirectFixerPlugin{

    var changeNextRequestMethod = AtomicBoolean(false)
    @KtorDsl
    public class Config{

    }

    public companion object Plugin : HttpClientPlugin<Config, RedirectFixerPlugin>{
        override val key: AttributeKey<RedirectFixerPlugin>
            get() = AttributeKey("RedirectFixerPlugin")

        override fun prepare(block: Config.() -> Unit): RedirectFixerPlugin {
            return RedirectFixerPlugin()
        }

        override fun install(plugin: RedirectFixerPlugin, scope: HttpClient) {
            Log.d("PLUGIN INSTALLATION","Installing plugin for client")
            scope.plugin(HttpSend).intercept { context ->
                if(plugin.changeNextRequestMethod.get()){
                    plugin.changeNextRequestMethod.set(false)
                    context.method = HttpMethod.Get
                }
                val isPostRequest = (context.method == HttpMethod.Post)
                val v = execute(context)
                if(isPostRequest && v.response.status == HttpStatusCode.Found){
                    plugin.changeNextRequestMethod.set(true)
                }
                v
            }
        }

    }
}