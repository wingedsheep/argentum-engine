package com.wingedsheep.mtg.sets

import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.FunSpec
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Validates that every card in every set has an imageUri that points to a real, reachable image.
 *
 * Disabled by default (requires network access and is slow).
 * Run explicitly with: ./gradlew :mtg-sets:test --tests "*.CardImageUriTest" -DverifyImageUris=true
 */
class VerifyImageUrisCondition : io.kotest.core.annotation.EnabledCondition {
    override fun enabled(kclass: kotlin.reflect.KClass<out io.kotest.core.spec.Spec>): Boolean =
        System.getProperty("verifyImageUris") == "true"
}

@EnabledIf(VerifyImageUrisCondition::class)
class CardImageUriTest : FunSpec({

    val allCards: List<Pair<String, CardDefinition>> =
        PortalSet.allCards.map { "Portal" to it } +
        OnslaughtSet.allCards.map { "Onslaught" to it }

    val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    test("every card should have a non-null imageUri") {
        val missing = allCards.filter { (_, card) -> card.metadata.imageUri == null }
        if (missing.isNotEmpty()) {
            val names = missing.joinToString("\n") { (set, card) ->
                "  $set: ${card.name} (collector #${card.metadata.collectorNumber})"
            }
            throw AssertionError("${missing.size} card(s) missing imageUri:\n$names")
        }
    }

    test("every card imageUri should return HTTP 200") {
        val failures = mutableListOf<String>()

        for ((set, card) in allCards) {
            val uri = card.metadata.imageUri ?: continue
            val request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build()
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.discarding())
                if (response.statusCode() !in 200..299) {
                    failures += "$set: ${card.name} (collector #${card.metadata.collectorNumber}) -> HTTP ${response.statusCode()} for $uri"
                }
            } catch (e: Exception) {
                failures += "$set: ${card.name} (collector #${card.metadata.collectorNumber}) -> ${e.javaClass.simpleName}: ${e.message} for $uri"
            }
        }

        if (failures.isNotEmpty()) {
            throw AssertionError("${failures.size} card image URI(s) unreachable:\n${failures.joinToString("\n") { "  $it" }}")
        }
    }
})
