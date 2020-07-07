package com.edt.ut3.backend.requests

object RequestsUtils {
    
    class RequestBody {
        private val body = StringBuilder()

        init {
            this.add("resType", 103)
                .add("calView", "agendaDay")
                .add("colourScheme", 3)
        }

        fun add(key: String, value: Any) = apply {
            if (body.isNotEmpty()) {
                body.append("&")
            }

            body.append(key).append("=").append(value.toString())
        }

        fun build() = body.toString()
    }
}