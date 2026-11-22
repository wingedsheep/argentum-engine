package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Keymaster Rogue (GTC) — can't be blocked; when it enters, return a creature you control to its
 * owner's hand (chosen on resolution, not targeted).
 */
class KeymasterRogueScenarioTest : ScenarioTestBase() {
    init {
        test("enters, returns another creature you control, and is unblockable") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Keymaster Rogue")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Keymaster Rogue").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.contains(game.findPermanent("Hill Giant")!!) shouldBe false
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            val rogue = game.findPermanent("Keymaster Rogue")!!
            game.state.projectedState.hasKeyword(rogue, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }

        test("alone, it must return itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Keymaster Rogue")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Keymaster Rogue").error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Keymaster Rogue") shouldBe true
        }
    }
}
