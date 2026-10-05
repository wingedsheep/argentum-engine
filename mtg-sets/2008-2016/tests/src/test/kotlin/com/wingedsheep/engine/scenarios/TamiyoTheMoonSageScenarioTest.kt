package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tamiyo, the Moon Sage (AVR #79, {3}{U}{U}, loyalty 4).
 *
 *   +1: Tap target permanent. It doesn't untap during its controller's next untap step.
 *   −2: Draw a card for each tapped creature target player controls.
 *   −8: You get an emblem with "You have no maximum hand size" and "Whenever a card is put into
 *       your graveyard from anywhere, you may return it to your hand."
 *
 * The +1's rider has to outlive the target controller's untap step and then wear off; the −2 counts
 * only the *target player's tapped creatures* and draws for Tamiyo's controller; the −8's emblem
 * trigger is a "may" that returns the card on yes and leaves it on no.
 */
class TamiyoTheMoonSageScenarioTest : ScenarioTestBase() {

    init {
        context("the +1") {

            test("taps the target and it skips its controller's next untap step only") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, the Moon Sage")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, the Moon Sage")!!
                setLoyalty(game, tamiyo, 4)
                val bears = game.findPermanent("Grizzly Bears")!!

                activate(game, tamiyo, index = 0, targets = listOf(ChosenTarget.Permanent(bears)))
                game.resolveStack()

                withClue("bears tapped; loyalty 4 -> 5") {
                    isTapped(game, bears) shouldBe true
                    loyalty(game, tamiyo) shouldBe 5
                }

                advanceToNextTurn(game)
                withClue("the opponent's untap step did not untap the bears") {
                    game.state.activePlayerId shouldBe game.player2Id
                    isTapped(game, bears) shouldBe true
                }

                advanceToNextTurn(game)
                advanceToNextTurn(game)
                withClue("the following untap step is unaffected") {
                    game.state.activePlayerId shouldBe game.player2Id
                    isTapped(game, bears) shouldBe false
                }
            }
        }

        context("the −2") {

            test("draws one card per tapped creature the target player controls") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, the Moon Sage")
                    .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Hill Giant")
                    // A tapped noncreature permanent must not count.
                    .withCardOnBattlefield(2, "Forest", tapped = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, the Moon Sage")!!
                setLoyalty(game, tamiyo, 4)
                val handBefore = game.handSize(1)
                val opponentHandBefore = game.handSize(2)

                activate(game, tamiyo, index = 1, targets = listOf(ChosenTarget.Player(game.player2Id)))
                game.resolveStack()

                withClue("two tapped opposing creatures -> controller draws two; loyalty 4 -> 2") {
                    game.handSize(1) shouldBe handBefore + 2
                    game.handSize(2) shouldBe opponentHandBefore
                    loyalty(game, tamiyo) shouldBe 2
                }
            }
        }

        context("the −8 emblem") {

            test("a card put into your graveyard may be returned to hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, the Moon Sage")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, the Moon Sage")!!
                setLoyalty(game, tamiyo, 10)

                activate(game, tamiyo, index = 2)
                game.resolveStack()
                withClue("emblem created; Tamiyo survives at 2") {
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                    loyalty(game, tamiyo) shouldBe 2
                }

                // Yes: the resolved Bolt comes back to hand.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                withClue("Bolt dealt 3 and the emblem asks to return it") {
                    game.getLifeTotal(2) shouldBe 17
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()
                withClue("accepted: the Bolt is back in hand, not in the graveyard") {
                    game.findCardsInHand(1, "Lightning Bolt").size shouldBe 2
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                }

                // No: the card stays in the graveyard.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 14
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()
                withClue("declined: the Bolt stays in the graveyard") {
                    game.findCardsInHand(1, "Lightning Bolt").size shouldBe 1
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
            }

            test("activated from exactly 8, Tamiyo is already gone when the emblem appears") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, the Moon Sage")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, the Moon Sage")!!
                setLoyalty(game, tamiyo, 8)

                activate(game, tamiyo, index = 2)
                game.resolveStack()

                withClue("she died before the emblem existed, so it never offers to return her") {
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(1, "Tamiyo, the Moon Sage") shouldBe true
                }
            }
        }
    }

    /** Step out through the end step so the next [Phase.PRECOMBAT_MAIN] is the *following* turn's. */
    private fun advanceToNextTurn(game: TestGame) {
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList()
    ) {
        val ability = cardRegistry.getCard("Tamiyo, the Moon Sage")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true
}
