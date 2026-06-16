package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the SOS conditional / "if" spells:
 *  - Ajani's Response — {3}-less cost reduction when it targets a tapped creature; destroys it.
 *  - Homesickness — target player draws two; tap up to two target creatures + stun each.
 *  - Moment of Reckoning — choose up to four (repeatable): destroy nonland / reanimate nonland permanent.
 */
class SosConditionalSpellsScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.stunCount(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun ScenarioTestBase.TestGame.isTapped(entityId: EntityId): Boolean =
        state.getEntity(entityId)?.has<TappedComponent>() == true

    init {

        context("Ajani's Response") {

            test("costs {3} less when targeting a tapped creature and destroys it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani's Response")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2) // only 2 mana: needs reduction to afford
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                // Tap the target so the {3} reduction applies (full cost is {4}{W}, reduced to {1}{W}).
                game.state = game.state.updateEntity(bears) { it.with(TappedComponent) }

                game.castSpell(1, "Ajani's Response", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("tapped creature is destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("destroys an untapped creature at full cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani's Response")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5) // full {4}{W}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Ajani's Response", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("untapped creature is destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }

        context("Homesickness") {

            test("target player draws two; both target creatures tapped and stunned") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Homesickness")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val handBefore = game.handSize(1)

                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Homesickness"
                }
                // Target self (player 1) to draw two, plus both opposing creatures.
                game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(giant),
                        ),
                    ),
                ).error shouldBe null
                game.resolveStack()

                withClue("caster drew two cards (minus the Homesickness that left hand)") {
                    game.handSize(1) shouldBe handBefore - 1 + 2
                }
                withClue("Grizzly Bears tapped + 1 stun counter") {
                    game.isTapped(bears) shouldBe true
                    game.stunCount(bears) shouldBe 1
                }
                withClue("Hill Giant tapped + 1 stun counter") {
                    game.isTapped(giant) shouldBe true
                    game.stunCount(giant) shouldBe 1
                }
            }
        }

        context("Moment of Reckoning") {

            test("destroy mode chosen twice destroys two nonland permanents") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Moment of Reckoning")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Moment of Reckoning"
                }

                // Choose the "destroy" mode (index 0) twice, each with its own target.
                game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                        chosenModes = listOf(0, 0),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears)),
                            listOf(ChosenTarget.Permanent(giant)),
                        ),
                    ),
                ).error shouldBe null
                game.resolveStack()

                withClue("both nonland permanents destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
