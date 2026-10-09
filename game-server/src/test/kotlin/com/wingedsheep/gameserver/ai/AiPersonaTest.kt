package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.profile.Avatars
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe

class AiPersonaTest : FunSpec({

    test("every AI persona sits behind its own AI portrait, and every AI portrait has a persona") {
        AiGameManager.AI_PERSONAS.map { it.avatar }.toSet() shouldBe Avatars.aiIds
    }

    test("AI portraits are not presets a player can pick") {
        Avatars.aiIds.filter { Avatars.isPreset(it) }.shouldBeEmpty()
    }

    test("AI personas have distinct names and portraits") {
        AiGameManager.AI_PERSONAS.map { it.name }.shouldNotContainDuplicates()
        AiGameManager.AI_PERSONAS.map { it.avatar }.shouldNotContainDuplicates()
    }
})
