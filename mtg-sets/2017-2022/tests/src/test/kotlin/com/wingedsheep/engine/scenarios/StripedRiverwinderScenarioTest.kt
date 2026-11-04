package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class StripedRiverwinderScenarioTest : ScenarioTestBase() {
    init {
        test("cycles for a single blue mana and draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Striped Riverwinder").withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.cycleCard(1, "Striped Riverwinder").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Striped Riverwinder") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
        test("hexproof rejects an opponent's targeted spell") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Striped Riverwinder")
                .withCardInHand(1, "Murder").withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Murder", game.findPermanent("Striped Riverwinder")!!).error shouldNotBe null
            game.isOnBattlefield("Striped Riverwinder") shouldBe true
            game.isInHand(1, "Murder") shouldBe true
        }
    }
}
