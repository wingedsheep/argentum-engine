package com.wingedsheep.engine.hygiene

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class EscapeCastSerializationTest : StringSpec({
    "the client-selected escape ability survives an action round trip" {
        val json = Json { serializersModule = engineSerializersModule }
        val original: GameAction = CastSpell(
            EntityId.generate(), EntityId.generate(), useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.ESCAPE,
            escapeAbility = KeywordAbility.Escape(ManaCost.parse("{3}{R}"), Costs.additional.ExileOtherCards(2))
        )
        json.decodeFromString(GameAction.serializer(),
            json.encodeToString(GameAction.serializer(), original)) shouldBe original
    }
})
