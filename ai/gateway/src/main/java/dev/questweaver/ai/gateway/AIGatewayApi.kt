package dev.questweaver.ai.gateway

import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.serialization.Serializable

interface AIGatewayApi {
    @POST("v1/narrate")
    suspend fun narrate(@Body req: NarrateReq): NarrateResp
    @POST("v1/intent")
    suspend fun intent(@Body req: IntentReq): IntentResp
}

@Serializable data class NarrateReq(val state: String, val prompt: String)
@Serializable data class NarrateResp(val text: String)
@Serializable data class IntentReq(val text: String)
@Serializable data class IntentResp(val label: String)
