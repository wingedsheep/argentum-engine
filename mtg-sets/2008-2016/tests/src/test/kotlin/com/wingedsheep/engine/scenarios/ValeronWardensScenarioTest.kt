package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Valeron Wardens (ORI #203) — {2}{G} Creature — Human Monk 1/3.
 *
 *   Renown 2
 *   Whenever a creature you control becomes renowned, draw a card.
 *
 * The first card to watch *other* creatures become renowned: `Triggers.a(Creature.youControl())
 * .becomesRenowned()` must fire for another creature you control, for the Wardens itself, and not
 * for an opponent's creature.
 */
class ValeronWardensScenarioTest : ScenarioTestBase() {

    /** [attackerController]'s [attacker] connects unblocked; returns the game once the stack is empty. */
    private fun connect(attackerController: Int, attacker: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Valeron Wardens")
            .withCardOnBattlefield(attackerController, "Rhox Maulers")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(attackerController)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        val defender = if (attackerController == 1) 2 else 1
        game.declareAttackers(mapOf(attacker to defender)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers()
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var guard = 0
        while (game.state.stack.isNotEmpty() && guard++ < 10) game.resolveStack()
        return game
    }

    private fun TestGame.isRenowned(name: String): Boolean =
        state.getEntity(findPermanent(name)!!)?.has<RenownedComponent>() == true

    init {
        test("another creature you control becoming renowned draws a card") {
            val game = connect(attackerController = 1, attacker = "Rhox Maulers")

            game.isRenowned("Rhox Maulers") shouldBe true
            withClue("one card drawn off the Maulers becoming renowned") {
                game.handSize(1) shouldBe 1
            }
        }

        test("Valeron Wardens becoming renowned itself draws a card") {
            val game = connect(attackerController = 1, attacker = "Valeron Wardens")

            game.isRenowned("Valeron Wardens") shouldBe true
            game.handSize(1) shouldBe 1
        }

        test("an opponent's creature becoming renowned draws nothing") {
            val game = connect(attackerController = 2, attacker = "Rhox Maulers")

            game.isRenowned("Rhox Maulers") shouldBe true
            withClue("the Wardens' controller draws no card for an opponent's creature") {
                game.handSize(1) shouldBe 0
            }
        }
    }
}
