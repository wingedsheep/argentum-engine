package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.isd.cards.LilianaOfTheVeil
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Liliana of the Veil (ISD #105, {1}{B}{B}, Loyalty 3).
 *
 *   +1: Each player discards a card.
 *   −2: Target player sacrifices a creature.
 *   −6: Separate all permanents target player controls into two piles. That player sacrifices all
 *       permanents in the pile of their choice.
 *
 * The +1 is symmetric (Liliana's controller discards too — easy to model as "each opponent" by
 * accident). The −2 targets a *player*, so the victim picks the creature and hexproof on their
 * board is irrelevant. The −6 is the interesting one: two different players decide, in order —
 * Liliana's controller partitions, then the *targeted* player picks which pile dies.
 */
class LilianaOfTheVeilScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(LilianaOfTheVeil))

        context("the +1") {

            test("is symmetric — both players discard, including Liliana's controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana of the Veil")
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana of the Veil")!!
                setLoyalty(game, liliana, 3)

                activate(game, liliana, index = 0)
                game.resolveStack()

                // Each player is asked in turn; with a single card in hand the pick is forced.
                repeat(2) {
                    val decision = game.state.pendingDecision
                    if (decision is SelectCardsDecision) {
                        game.selectCards(listOf(decision.options.first()))
                    }
                    game.resolveStack()
                }

                withClue("the controller discarded too — this is not \"each opponent\"") {
                    game.isInGraveyard(1, "Savannah Lions") shouldBe true
                }
                withClue("and so did the opponent") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
                withClue("+1 took Liliana from 3 to 4") { loyalty(game, liliana) shouldBe 4 }
            }
        }

        context("the −2") {

            test("the targeted player sacrifices a creature of their choice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana of the Veil")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana of the Veil")!!
                setLoyalty(game, liliana, 3)

                activate(game, liliana, index = 1, targetPlayer = game.player2Id)
                game.resolveStack()

                // Sole creature — the sacrifice is forced, but answer the prompt if one appears.
                val decision = game.state.pendingDecision
                if (decision is SelectCardsDecision) {
                    game.selectCards(listOf(decision.options.first()))
                    game.resolveStack()
                }

                withClue("their only creature was sacrificed") {
                    game.isInGraveyard(2, "Savannah Lions") shouldBe true
                }
                withClue("−2 took Liliana from 3 to 1") { loyalty(game, liliana) shouldBe 1 }
            }
        }

        context("the −6 pile split") {

            test("controller partitions, the targeted player picks, and that pile is sacrificed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana of the Veil")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana of the Veil")!!
                setLoyalty(game, liliana, 6)

                val lions = game.findPermanent("Savannah Lions")!!

                activate(game, liliana, index = 2, targetPlayer = game.player2Id)
                game.resolveStack()

                // Decision 1 — Liliana's controller puts the Lions in pile 1, the Courser in pile 2.
                withClue("the partition is asked of Liliana's controller") {
                    (game.state.pendingDecision as? SelectCardsDecision)
                        ?.playerId shouldBe game.player1Id
                }
                game.selectCards(listOf(lions))
                game.resolveStack()

                // Decision 2 — the targeted player chooses which pile they sacrifice.
                val pileChoice = game.state.pendingDecision as? ChooseOptionDecision
                withClue("the pile choice belongs to the targeted player, not the controller") {
                    pileChoice?.playerId shouldBe game.player2Id
                }
                // They pick pile 2 (the Courser), keeping the Lions.
                game.submitDecision(OptionChosenResponse(pileChoice!!.id, 1))
                game.resolveStack()

                withClue("the chosen pile was sacrificed") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
                withClue("the other pile survived") {
                    game.isInGraveyard(2, "Savannah Lions") shouldBe false
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
            }

            test("an empty pile is legal and sacrifices nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Liliana of the Veil")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana of the Veil")!!
                setLoyalty(game, liliana, 6)

                activate(game, liliana, index = 2, targetPlayer = game.player2Id)
                game.resolveStack()

                // Controller selects nothing, so pile 1 is empty and pile 2 holds everything.
                game.skipSelection()
                game.resolveStack()

                val pileChoice = game.state.pendingDecision as ChooseOptionDecision
                // The opponent takes the empty pile.
                game.submitDecision(OptionChosenResponse(pileChoice.id, 0))
                game.resolveStack()

                withClue("choosing the empty pile sacrifices nothing (CR 700.3d)") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.isInGraveyard(2, "Savannah Lions") shouldBe false
                }
            }
        }
    }

    /**
     * Loyalty abilities choose their targets as they are activated (CR 601.2c), so the targeted
     * player is passed in here rather than answered as a decision afterwards.
     */
    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targetPlayer: EntityId? = null
    ) {
        val ability = cardRegistry.getCard("Liliana of the Veil")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targetPlayer?.let { listOf(ChosenTarget.Player(it)) } ?: emptyList()
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
}
