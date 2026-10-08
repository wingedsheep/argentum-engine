package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Powerstone Fracture (BRO #112) — additional cost: sacrifice an artifact or creature;
 * destroy target creature or planeswalker.
 */
class PowerstoneFractureScenarioTest : ScenarioTestBase() {

    init {
        test("sacrificing an artifact pays the cost and destroys the target creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Powerstone Fracture")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Powerstone Fracture").single()
            val fodder = game.findPermanent("Ornithopter")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(ChosenTarget.Permanent(bears)),
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Ornithopter") shouldBe true

            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Powerstone Fracture") shouldBe true
        }

        test("cannot be cast without an artifact or creature to sacrifice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Powerstone Fracture")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Powerstone Fracture").single()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                CastSpell(game.player1Id, spell, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
