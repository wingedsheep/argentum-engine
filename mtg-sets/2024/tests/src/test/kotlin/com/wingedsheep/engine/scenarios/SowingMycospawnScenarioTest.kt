package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe

class SowingMycospawnScenarioTest : ScenarioTestBase() {
    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Sowing Mycospawn")
        .withCardInHand(1, "Counterspell")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withLandsOnBattlefield(1, "Snow-Covered Wastes", 1)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardOnBattlefield(2, "Mountain")
        .withCardInLibrary(1, "Snow-Covered Wastes")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

    private fun TestGame.cast(kicked: Boolean) {
        execute(CastSpell(player1Id, findCardsInHand(1, "Sowing Mycospawn").single(),
            declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null)).error shouldBe null
        (getPendingDecision() as? OrderObjectsDecision)?.let {
            submitDecision(OrderedResponse(it.id, it.objects)).error shouldBe null
        }
        if (kicked) selectTargets(listOf(findPermanent("Mountain")!!)).error shouldBe null
    }

    init {
        test("unkicked cast searches for an untapped land before the creature resolves without an exile target") {
            val game = board()
            game.cast(false)
            game.state.stack.size shouldBe 2
            game.resolveStack()
            game.selectCards(listOf(game.findCardsInLibrary(1, "Snow-Covered Wastes").single())).error shouldBe null
            game.isOnBattlefield("Sowing Mycospawn") shouldBe false
            game.findPermanents("Snow-Covered Wastes").size shouldBe 2
            game.resolveStack()
            game.isOnBattlefield("Sowing Mycospawn") shouldBe true
            game.isOnBattlefield("Mountain") shouldBe true
        }
        test("kicked cast exiles a land and still searches after its creature spell is countered") {
            val game = board()
            game.cast(true)
            game.state.stack.size shouldBe 3
            game.castSpellTargetingStackSpell(1, "Counterspell", "Sowing Mycospawn").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(game.findCardsInLibrary(1, "Snow-Covered Wastes").single())).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Sowing Mycospawn") shouldBe true
            game.isInExile(2, "Mountain") shouldBe true
            game.findPermanents("Snow-Covered Wastes").size shouldBe 2
        }
        test("search may fail to find a land and putting the creature directly onto the battlefield does not trigger") {
            val game = board()
            game.cast(false)
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()
            game.findPermanents("Snow-Covered Wastes").size shouldBe 1
            val direct = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Sowing Mycospawn").build()
            direct.state.stack.size shouldBe 0
        }
    }
}
