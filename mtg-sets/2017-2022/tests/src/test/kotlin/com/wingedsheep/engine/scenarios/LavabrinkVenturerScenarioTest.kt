package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lavabrink Venturer (IKO #19).
 *
 * "As this creature enters, choose odd or even. (Zero is even.) / This creature has protection
 *  from each mana value of the chosen quality."
 *
 * Covers both choices, a spell's announced X counting toward its mana value (ruling), and the
 * blocking leg of protection.
 */
class LavabrinkVenturerScenarioTest : ScenarioTestBase() {

    private val venturer = "Lavabrink Venturer"

    private fun TestGame.castVenturerChoosing(parity: String) {
        castSpell(1, venturer).error shouldBe null
        resolveStack()
        val decision = getPendingDecision() as? ChooseOptionDecision
            ?: error("Expected the odd-or-even choice, got ${getPendingDecision()}")
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(parity))).error shouldBe null
        resolveStack()
        isOnBattlefield(venturer) shouldBe true
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, venturer)
        .withCardInHand(1, "Lightning Bolt")
        .withCardInHand(1, "Volcanic Hammer")
        .withCardInHand(1, "Blaze")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("odd chosen: odd mana value spells can't target it, even ones can") {
            val game = board().build()
            game.castVenturerChoosing("Odd")
            val id = game.findPermanent(venturer)

            withClue("Lightning Bolt (mana value 1) is odd") {
                game.castSpell(1, "Lightning Bolt", id).error shouldNotBe null
            }
            withClue("Volcanic Hammer (mana value 2) is even") {
                game.castSpell(1, "Volcanic Hammer", id).error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield(venturer) shouldBe false
        }

        test("even chosen: Blaze's announced X decides its mana value") {
            val game = board().build()
            game.castVenturerChoosing("Even")
            val id = game.findPermanent(venturer)

            withClue("Blaze with X=1 has mana value 2 — even, so it can't target") {
                game.castXSpell(1, "Blaze", 1, id).error shouldNotBe null
            }
            withClue("Blaze with X=2 has mana value 3 — odd, so it can") {
                game.castXSpell(1, "Blaze", 2, id).error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield(venturer) shouldBe true
            withClue("Lightning Bolt (mana value 1) can target it too") {
                game.castSpell(1, "Lightning Bolt", id).error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield(venturer) shouldBe false
        }

        test("even chosen: damage from an even source is prevented, and an even creature can't block it") {
            val game = board()
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .build()
            game.castVenturerChoosing("Even")

            // Opponent's turn: Grizzly Bears (mana value 2) attacks into the Venturer.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf(venturer to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("the Bears' damage was prevented; the Venturer's killed them") {
                (game.state.getEntity(game.findPermanent(venturer)!!)?.get<DamageComponent>()?.amount ?: 0).shouldBe(0)
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            // Our turn: the Venturer attacks.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(venturer to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Hill Giant (mana value 4) can't block it") {
                game.declareBlockers(mapOf("Hill Giant" to listOf(venturer))).error shouldNotBe null
            }
            withClue("Savannah Lions (mana value 1) can") {
                game.declareBlockers(mapOf("Savannah Lions" to listOf(venturer))).error shouldBe null
            }
        }
    }
}
