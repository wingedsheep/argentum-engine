package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Riptide Chimera (JOU #48) — {2}{U} Enchantment Creature — Chimera 3/4.
 *
 *   Flying
 *   At the beginning of your upkeep, return an enchantment you control to its owner's hand.
 *
 * The return is mandatory and untargeted; only enchantments you control are offered, the Chimera
 * itself among them.
 */
class RiptideChimeraScenarioTest : ScenarioTestBase() {
    init {
        test("your upkeep returns the enchantment you choose; non-enchantments aren't offered") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Riptide Chimera")
                .withCardOnBattlefield(1, "Glorious Anthem")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Glorious Anthem")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myAnthem = game.findPermanents("Glorious Anthem")
                .single { it in game.state.getBattlefield(game.player1Id) }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only enchantments you control are offered, the Chimera included") {
                decision.options.toSet() shouldBe setOf(game.findPermanent("Riptide Chimera")!!, myAnthem)
            }
            game.selectCards(listOf(myAnthem)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Glorious Anthem") shouldBe true
            game.isOnBattlefield("Riptide Chimera") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
