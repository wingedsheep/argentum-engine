package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Angrath, the Flame-Chained (RIX #152, {3}{B}{R}, Loyalty 4).
 *
 *   +1: Each opponent discards a card and loses 2 life.
 *   −3: Gain control of target creature until end of turn. Untap it. It gains haste until end of
 *   turn. Sacrifice it at the beginning of the next end step if it has mana value 3 or less.
 *   −8: Each opponent loses life equal to the number of cards in their graveyard.
 *
 * Covers the +1 with and without a card to discard, the −3 sacrificing a stolen mana value 2
 * creature at the end step but returning a mana value 4 one, and the −8 counting only the
 * opponent's own graveyard.
 */
class AngrathTheFlameChainedScenarioTest : ScenarioTestBase() {

    init {
        context("+1") {
            test("the opponent discards a card and loses 2 life") {
                val game = board().withCardInHand(2, "Grizzly Bears").build()
                val angrath = game.findPermanent("Angrath, the Flame-Chained")!!

                activate(game, angrath, index = 0)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectCards(game.findCardsInHand(2, "Grizzly Bears"))
                }

                game.handSize(2) shouldBe 0
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 18
            }

            test("an opponent with an empty hand still loses 2 life") {
                val game = board().build()
                val angrath = game.findPermanent("Angrath, the Flame-Chained")!!

                activate(game, angrath, index = 0)
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
            }
        }

        context("−3") {
            test("steals, untaps and hastes a creature, then sacrifices it at the end step when its mana value is 3 or less") {
                val game = board().withCardOnBattlefield(2, "Grizzly Bears", tapped = true).build()
                val angrath = game.findPermanent("Angrath, the Flame-Chained")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                activate(game, angrath, index = 1, target = bears)
                game.resolveStack()

                withClue("control changed, untapped, haste") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the mana value 2 creature was sacrificed into its owner's graveyard") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("a creature with mana value greater than 3 is not sacrificed and returns to its owner") {
                val game = board().withCardOnBattlefield(2, "Hill Giant").build()
                val angrath = game.findPermanent("Angrath, the Flame-Chained")!!
                val giant = game.findPermanent("Hill Giant")!!

                activate(game, angrath, index = 1, target = giant)
                game.resolveStack()
                game.state.projectedState.getController(giant) shouldBe game.player1Id

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.isOnBattlefield("Hill Giant") shouldBe true

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.state.projectedState.getController(giant) shouldBe game.player2Id
            }
        }

        context("−8") {
            test("the opponent loses life equal to the cards in their own graveyard") {
                val game = board()
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .build()
                val angrath = game.findPermanent("Angrath, the Flame-Chained")!!
                setLoyalty(game, angrath, 8)

                activate(game, angrath, index = 2)
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Angrath, the Flame-Chained")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun activate(game: TestGame, source: EntityId, index: Int, target: EntityId? = null) {
        val ability = cardRegistry.getCard("Angrath, the Flame-Chained")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = listOfNotNull(target?.let { ChosenTarget.Permanent(it) }),
            )
        ).error shouldBe null
    }

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
