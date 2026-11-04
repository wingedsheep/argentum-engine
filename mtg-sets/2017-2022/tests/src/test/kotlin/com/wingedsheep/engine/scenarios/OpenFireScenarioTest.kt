package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget

class OpenFireScenarioTest : ScenarioTestBase() {
    init {
        test("can deal its 3 damage directly to an opponent") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Open Fire").withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val card = game.findCardsInHand(1, "Open Fire").single()
            game.execute(CastSpell(game.player1Id, card, listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
            game.isInGraveyard(1, "Open Fire") shouldBe true
        }
        test("kills a creature with exactly 3 toughness") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Open Fire").withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Open Fire", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }
    }
}
