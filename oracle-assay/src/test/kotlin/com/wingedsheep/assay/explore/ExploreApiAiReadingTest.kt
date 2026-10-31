package com.wingedsheep.assay.explore

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The explorer's AI reading hook, from the side that has no AI: standalone `assay explore` mounts
 * the route anyway and answers that the reading needs the game server, rather than 404ing or
 * failing — the page renders that as its "unavailable" state.
 */
class ExploreApiAiReadingTest : StringSpec({

    "without a provider the ai route answers unavailable, and status says so" {
        val api = ExploreApi()
        ("ai" in api.getRoutes) shouldBe true
        val answer = api.get("ai") { if (it == "name") "Lightning Bolt" else null } as JsonObject
        answer["available"] shouldBe JsonPrimitive(false)
        (api.get("status") as JsonObject)["aiReading"] shouldBe JsonPrimitive(false)
    }
})
