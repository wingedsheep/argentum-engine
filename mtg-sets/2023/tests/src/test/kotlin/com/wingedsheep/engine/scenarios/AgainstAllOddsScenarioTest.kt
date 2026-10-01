package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Against All Odds (ONE #1) — {3}{W} Sorcery.
 *
 *   Choose one or both —
 *   • Exile target artifact or creature you control, then return it to the battlefield under its
 *     owner's control.
 *   • Return target artifact or creature card with mana value 3 or less from your graveyard to the
 *     battlefield.
 */
class AgainstAllOddsScenarioTest : ScenarioTestBase() {

    private val flicker = 0
    private val reanimate = 1

    private fun TestGame.castOdds(modeTargets: List<Pair<Int, ChosenTarget>>): ExecutionResult {
        val cardId = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Against All Odds" }
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = cardId,
                targets = modeTargets.map { it.second },
                chosenModes = modeTargets.map { it.first },
                modeTargetsOrdered = modeTargets.map { listOf(it.second) },
            )
        )
    }

    private fun baseScenario() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Against All Odds")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Against All Odds") {

            test("mode 1 flickers a tapped creature: it returns untapped and its ETB fires") {
                val game = baseScenario()
                    .withCardOnBattlefield(1, "Elvish Visionary", tapped = true)
                    .build()

                val visionary = game.findPermanent("Elvish Visionary")!!
                val handBefore = game.handSize(1)

                game.castOdds(listOf(flicker to ChosenTarget.Permanent(visionary))).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Elvish Visionary")
                withClue("the creature is back on the battlefield") { returned shouldNotBe null }
                withClue("it returns untapped") {
                    game.state.getEntity(returned!!)
                        ?.has<TappedComponent>() shouldBe false
                }
                withClue("ETB draw fired (hand: -Against All Odds +1 card)") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("mode 1 can't target a creature an opponent controls") {
                val game = baseScenario()
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castOdds(listOf(flicker to ChosenTarget.Permanent(bears))).error shouldNotBe null
            }

            test("mode 2 returns a mana value 3 or less creature card from your graveyard") {
                val game = baseScenario()
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.castOdds(listOf(reanimate to ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)))
                    .error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }

            test("mode 2 can't target a mana value 4 creature card") {
                val game = baseScenario()
                    .withCardInGraveyard(1, "Hill Giant")
                    .build()

                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                game.castOdds(listOf(reanimate to ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD)))
                    .error shouldNotBe null
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }

            test("choosing both modes flickers one creature and reanimates another") {
                val game = baseScenario()
                    .withCardOnBattlefield(1, "Elvish Visionary", tapped = true)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .build()

                val visionary = game.findPermanent("Elvish Visionary")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val handBefore = game.handSize(1)

                game.castOdds(
                    listOf(
                        flicker to ChosenTarget.Permanent(visionary),
                        reanimate to ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                    )
                ).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Elvish Visionary")
                returned shouldNotBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe handBefore
            }
        }
    }
}
