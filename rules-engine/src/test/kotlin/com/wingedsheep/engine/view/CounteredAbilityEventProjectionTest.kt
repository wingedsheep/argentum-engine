package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.AbilityCounteredEvent
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CounteredAbilityEventProjectionTest : FunSpec({
    test("client projection omits internal countered-ability source metadata") {
        val event = AbilityCounteredEvent(
            abilityEntityId = EntityId.of("internal-ability"),
            description = "Ward ability",
            sourceId = EntityId.of("internal-source"),
            sourceName = "Hidden Source Name",
            controllerId = EntityId.of("internal-controller"),
        )

        val projected = ClientEventTransformer.transform(
            events = listOf(event),
            viewingPlayerId = EntityId.of("viewer"),
        ).single()

        projected shouldBe ClientEvent.AbilityCountered(abilityDescription = "Ward ability")
        val wirePayload = Json.encodeToString<ClientEvent>(projected)
        wirePayload shouldNotContain "internal-ability"
        wirePayload shouldNotContain "internal-source"
        wirePayload shouldNotContain "Hidden Source Name"
        wirePayload shouldNotContain "internal-controller"
    }
})
