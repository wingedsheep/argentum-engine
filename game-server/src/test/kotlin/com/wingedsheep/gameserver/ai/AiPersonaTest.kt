package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.profile.Avatars
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContainDuplicates

class AiPersonaTest : FunSpec({

    test("every AI persona sits behind a preset portrait the client has art for") {
        AiGameManager.AI_PERSONAS.filterNot { Avatars.isPreset(it.avatar) }.shouldBeEmpty()
    }

    test("AI personas have distinct names and portraits") {
        AiGameManager.AI_PERSONAS.map { it.name }.shouldNotContainDuplicates()
        AiGameManager.AI_PERSONAS.map { it.avatar }.shouldNotContainDuplicates()
    }
})
