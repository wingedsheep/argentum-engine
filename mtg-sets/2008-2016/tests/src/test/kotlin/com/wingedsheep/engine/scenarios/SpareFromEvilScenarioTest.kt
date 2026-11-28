package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spare from Evil (ISD #34).
 *
 * "Creatures you control gain protection from non-Human creatures until end of turn."
 *
 * The quality is the complement of a subtype narrowed to a card type: a non-Human creature can't
 * block (or damage) a protected creature, a Human creature still can, and a noncreature spell
 * isn't a creature at all, so it still targets.
 */
class SpareFromEvilScenarioTest : ScenarioTestBase() {

    private val keyword = "PROTECTION_FROM_SUBTYPE_!HUMAN@CREATURE"

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Spare from Evil")
        .withCardInHand(2, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(2, "Mountain", 1)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(2, "Elite Vanguard", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castSpare() {
        castSpell(1, "Spare from Evil").error shouldBe null
        resolveStack()
    }

    init {
        test("creatures you control gain the protection; the opponent's don't") {
            val game = board()
            game.castSpare()
            game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, keyword) shouldBe true
            game.state.projectedState.hasKeyword(game.findPermanent("Hill Giant")!!, keyword) shouldBe false
        }

        test("a non-Human creature can't block a protected creature, but a Human can") {
            val game = board()
            game.castSpare()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Hill Giant is a non-Human creature") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
            withClue("Elite Vanguard is a Human, so it can block") {
                game.declareBlockers(mapOf("Elite Vanguard" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }

        test("the protection ends at end of turn") {
            val game = board()
            game.castSpare()
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, keyword) shouldBe false
        }
    }
}
