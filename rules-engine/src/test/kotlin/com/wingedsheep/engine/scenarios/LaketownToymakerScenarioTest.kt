package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Lake-town Toymaker (HOB) — {3}{W} Creature — Human Artificer 3/4.
 *
 * "At the beginning of combat on your turn, if you've drawn two or more cards this turn, another
 *  target creature you control gets +3/+0 and gains first strike until end of turn."
 *
 * The draw clause is an intervening-if (CR 603.4), so with fewer than two cards drawn the ability
 * never goes on the stack at all — no target is even asked for. "Another" excludes the Toymaker
 * itself, and "you control" excludes the opponent's board.
 */
class LaketownToymakerScenarioTest : ScenarioTestBase() {

    init {
        context("Lake-town Toymaker") {

            test("it is a 3/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lake-town Toymaker")
                    .build()

                val toymaker = game.findPermanent("Lake-town Toymaker")!!
                game.state.projectedState.getPower(toymaker) shouldBe 3
                game.state.projectedState.getToughness(toymaker) shouldBe 4
            }

            test("with two cards drawn it pumps another creature you control and grants first strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lake-town Toymaker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardsDrawnThisTurn(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val decision = game.getPendingDecision()
                withClue("the intervening-if is met, so the ability asks for its target") {
                    (decision is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("+3/+0 and first strike until end of turn") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 2
                    game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                }
            }

            test("neither the Toymaker itself nor an opponent's creature is a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lake-town Toymaker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardsDrawnThisTurn(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val toymaker = game.findPermanent("Lake-town Toymaker")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val theirs = game.findPermanent("Hill Giant")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val legal = decision.legalTargets[0] ?: emptyList()
                withClue("'another target creature you control'") {
                    legal shouldContain bears
                    legal shouldNotContain toymaker
                    legal shouldNotContain theirs
                }
            }

            test("with only one card drawn the ability never triggers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lake-town Toymaker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardsDrawnThisTurn(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                withClue("the intervening-if fails, so nothing goes on the stack") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
            }
        }
    }
}
