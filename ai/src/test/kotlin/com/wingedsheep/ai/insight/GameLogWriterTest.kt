package com.wingedsheep.ai.insight

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class GameLogWriterTest : FunSpec({

    test("a chosen player target renders as its seat label while the acting player field is stripped") {
        val seat0 = EntityId("3f2a9c1e-0000-4000-8000-000000000000")
        val seat1 = EntityId("7b4d2e8f-0000-4000-8000-000000000001")
        val writer = GameLogWriter(listOf(seat0, seat1), LegalActionEnumerator.create(CardRegistry()))

        val action = CastSpell(playerId = seat0, cardId = EntityId("e42"), targets = listOf(ChosenTarget.Player(seat1)))
        val line = writer.humanize(GameState(), action.toString())

        line shouldContain "Player(Seat1)"
        line shouldNotContain "playerId="
        line shouldNotContain "Seat0"
    }
})
