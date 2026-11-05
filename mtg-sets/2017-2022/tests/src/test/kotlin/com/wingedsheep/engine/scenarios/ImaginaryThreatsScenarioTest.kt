package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Imaginary Threats (HOU #37).
 *
 *   Creatures target opponent controls attack this turn if able. During that player's next untap
 *   step, creatures they control don't untap.
 *
 * Cast on the opponent's turn before combat: their creature must attack, and at their next untap
 * step their creatures stay tapped (the attacker, and one that was already tapped) while their
 * lands untap as normal.
 */
class ImaginaryThreatsScenarioTest : ScenarioTestBase() {

    init {
        test("the opponent's creatures must attack, then don't untap during their next untap step") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Imaginary Threats")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }
                .build()

            val threats = game.findCardsInHand(1, "Imaginary Threats").single()
            game.execute(
                CastSpell(game.player1Id, threats, listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("it is still the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            withClue("the Bears can't stay home") {
                game.declareAttackers(emptyMap()).error shouldNotBe null
            }
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null

            // Through the caster's turn to the opponent's next turn.
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            // Already in a precombat main, so step off it first or the next pass is a no-op.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id

            fun tapped(name: String) = game.state.getEntity(game.findPermanent(name)!!)!!.has<TappedComponent>()
            withClue("the creature that attacked stays tapped") { tapped("Grizzly Bears") shouldBe true }
            withClue("a creature tapped before the spell, which didn't attack, stays tapped") {
                tapped("Hill Giant") shouldBe true
            }
            withClue("only creatures are held — the land untaps") {
                game.findPermanents("Forest").none {
                    game.state.getEntity(it)!!.has<TappedComponent>()
                } shouldBe true
            }
        }
    }
}
