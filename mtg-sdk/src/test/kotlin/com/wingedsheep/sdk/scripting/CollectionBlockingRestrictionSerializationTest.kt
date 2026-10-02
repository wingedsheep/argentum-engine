package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CollectionBlockingRestrictionSerializationTest : FunSpec({
    test("collection restriction round trips as an effect with its duration and alternative") {
        val effect: Effect = GrantCantBeBlockedExceptByCollectionEffect(
            EffectTarget.IterationEntity, "left", GameObjectFilter.Creature.withKeyword(Keyword.FLYING), Duration.EndOfCombat
        )
        Json.decodeFromString<Effect>(Json.encodeToString(Effect.serializer(), effect)) shouldBe effect
    }
})
