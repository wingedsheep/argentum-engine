package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wargling (HOB) — {1}{G} Creature — Wolf 2/2.
 * "Ferocious — Whenever this creature attacks while you control a creature with power 4 or
 *  greater, until end of turn, this creature gets +1/+0 and creatures you control gain trample."
 *
 * Two clauses with different scopes fire off one trigger: the +1/+0 is self-only, while the
 * trample grant covers the whole team (and must not leak to the opponent's creatures).
 */
class WarglingScenarioTest : ScenarioTestBase() {

    init {
        context("Wargling") {

            test("attacking with ferocious pumps itself and gives your team trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wargling")
                    .withCardOnBattlefield(1, "Force of Nature")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val wargling = game.findPermanent("Wargling")!!
                val ally = game.findPermanent("Force of Nature")!!
                val theirs = game.findPermanent("Grizzly Bears")!!

                withClue("nobody has trample beforehand") {
                    game.state.projectedState.hasKeyword(wargling, Keyword.TRAMPLE) shouldBe false
                    game.state.projectedState.hasKeyword(ally, Keyword.TRAMPLE) shouldBe false
                }

                game.declareAttackers(mapOf("Wargling" to 2)).error shouldBe null
                game.resolveStack()

                withClue("+1/+0 is self-only — power moves, toughness does not") {
                    game.state.projectedState.getPower(wargling) shouldBe 3
                    game.state.projectedState.getToughness(wargling) shouldBe 2
                }
                withClue("the trample grant covers creatures you control") {
                    game.state.projectedState.hasKeyword(wargling, Keyword.TRAMPLE) shouldBe true
                    game.state.projectedState.hasKeyword(ally, Keyword.TRAMPLE) shouldBe true
                }
                withClue("the ally is not pumped — only Wargling gets +1/+0") {
                    game.state.projectedState.getPower(ally) shouldBe 5
                }
                withClue("the opponent's creature gains nothing") {
                    game.state.projectedState.hasKeyword(theirs, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("attacking without a power-4 creature grants nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wargling")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val wargling = game.findPermanent("Wargling")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.declareAttackers(mapOf("Wargling" to 2)).error shouldBe null
                game.resolveStack()

                withClue("a 3/3 does not satisfy ferocious") {
                    game.state.projectedState.getPower(wargling) shouldBe 2
                    game.state.projectedState.hasKeyword(wargling, Keyword.TRAMPLE) shouldBe false
                    game.state.projectedState.hasKeyword(courser, Keyword.TRAMPLE) shouldBe false
                }
            }
        }
    }
}
