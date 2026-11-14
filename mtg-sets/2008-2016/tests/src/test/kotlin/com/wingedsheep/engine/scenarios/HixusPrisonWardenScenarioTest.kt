package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hixus, Prison Warden (ORI #19).
 *
 * "Whenever a creature deals combat damage to you, if Hixus entered this turn, exile that
 * creature until Hixus leaves the battlefield."
 *
 * Under test: the damage source becomes "that creature", the entered-this-turn intervening if
 * gates the trigger, the exile is linked to Hixus and undone when it leaves, and (ruling) nothing
 * is exiled if Hixus is gone before the trigger resolves.
 */
class HixusPrisonWardenScenarioTest : ScenarioTestBase() {

    private fun board(hixusInHand: Boolean) = scenario()
        .withPlayers("Player1", "Player2")
        .apply {
            if (hixusInHand) {
                withCardInHand(1, "Hixus, Prison Warden")
                withLandsOnBattlefield(1, "Plains", 5)
            } else {
                withCardOnBattlefield(1, "Hixus, Prison Warden")
            }
        }
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        .withCardInHand(2, "Go for the Throat")
        .withLandsOnBattlefield(2, "Swamp", 2)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(2)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    init {
        test("flashed in during combat, it exiles the creature that hit you until it leaves") {
            val game = board(hixusInHand = true)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passPriority() // the attacker passes; the defender gets priority

            game.castSpell(1, "Hixus, Prison Warden").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Hixus, Prison Warden") shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            withClue("the Bears dealt 2 and were exiled") {
                game.getLifeTotal(1) shouldBe 18
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val hixus = game.findPermanent("Hixus, Prison Warden")!!
            game.castSpell(2, "Go for the Throat", hixus).error shouldBe null
            game.resolveStack()

            withClue("Hixus left, so the Bears return under their owner's control") {
                game.isInGraveyard(1, "Hixus, Prison Warden") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }
        }

        test("a Hixus that didn't enter this turn doesn't trigger") {
            val game = board(hixusInHand = false)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            game.state.stack.isEmpty() shouldBe true
            game.getLifeTotal(1) shouldBe 18
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("if Hixus leaves before the trigger resolves, the creature isn't exiled") {
            val game = board(hixusInHand = true)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passPriority()
            game.castSpell(1, "Hixus, Prison Warden").error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            withClue("the exile trigger is waiting on the stack") { game.state.stack.size shouldBe 1 }

            // The active player answers the trigger by killing Hixus.
            val hixus = game.findPermanent("Hixus, Prison Warden")!!
            game.castSpell(2, "Go for the Throat", hixus).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Hixus, Prison Warden") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInExile(2, "Grizzly Bears") shouldBe false
        }
    }
}
