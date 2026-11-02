package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vraska, Golgari Queen (GRN #213, {2}{B}{G}, Loyalty 4).
 *
 *   +2: You may sacrifice another permanent. If you do, you gain 1 life and draw a card.
 *   −3: Destroy target nonland permanent with mana value 3 or less.
 *   −9: You get an emblem with "Whenever a creature you control deals combat damage to a player,
 *       that player loses the game."
 *
 * Covers the two compositions whose correctness depends on execution: the +2's "if you do" gate
 * (declining must neither gain nor draw), and the global emblem resolving "that player" to the
 * damaged player after Vraska herself is gone.
 */
class VraskaGolgariQueenScenarioTest : ScenarioTestBase() {

    init {
        context("the +2") {

            test("sacrificing another permanent gains 1 life and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraska, Golgari Queen")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vraska = game.findPermanent("Vraska, Golgari Queen")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                setLoyalty(game, vraska, 4)
                activate(game, vraska, index = 0)
                game.resolveStack()
                if (game.getPendingDecision() is YesNoDecision) game.answerYesNo(true)
                withClue("the sacrifice is chosen on resolution") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Bears were sacrificed, Vraska wasn't") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.findPermanent("Vraska, Golgari Queen") shouldBe vraska
                }
                withClue("if you do: gain 1, draw 1") {
                    game.getLifeTotal(1) shouldBe 21
                    game.handSize(1) shouldBe 1
                }
                withClue("+2 moved Vraska from 4 to 6 loyalty") {
                    loyalty(game, vraska) shouldBe 6
                }
            }

            test("declining the sacrifice gains nothing and draws nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraska, Golgari Queen")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vraska = game.findPermanent("Vraska, Golgari Queen")!!
                setLoyalty(game, vraska, 4)
                activate(game, vraska, index = 0)
                game.resolveStack()
                when (game.getPendingDecision()) {
                    is YesNoDecision -> game.answerYesNo(false)
                    is SelectCardsDecision -> game.selectCards(emptyList())
                    else -> {}
                }
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.getLifeTotal(1) shouldBe 20
                game.handSize(1) shouldBe 0
            }
        }

        context("the −9 emblem") {

            test("outlives Vraska and makes a player dealt combat damage lose the game") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraska, Golgari Queen")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vraska = game.findPermanent("Vraska, Golgari Queen")!!
                setLoyalty(game, vraska, 9)
                activate(game, vraska, index = 2)
                game.resolveStack()

                withClue("Vraska died at 0 loyalty; the emblem is global") {
                    game.findPermanent("Vraska, Golgari Queen") shouldBe null
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("the damaged opponent lost the game") {
                    game.state.gameOver shouldBe true
                    game.state.winnerId shouldBe game.player1Id
                }
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard("Vraska, Golgari Queen")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id)
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
