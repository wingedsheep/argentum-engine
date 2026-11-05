package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.SamutTheTested
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Samut, the Tested (HOU #144).
 *
 * −2 splits its 2 damage between a player and a creature as it is activated. −7 searches for up to
 * two creature and/or planeswalker cards — a non-matching card can't be taken — and a planeswalker
 * put onto the battlefield this way enters with its printed loyalty.
 */
class SamutTheTestedScenarioTest : ScenarioTestBase() {

    private val minusTwo = SamutTheTested.activatedAbilities[1].id
    private val minusSeven = SamutTheTested.activatedAbilities[2].id

    private fun TestGame.setLoyalty(id: EntityId, loyalty: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) }
    }

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        context("Samut, the Tested") {

            test("−2 deals 1 damage to each of two targets, a player and a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Samut, the Tested")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val samut = game.findPermanent("Samut, the Tested")!!
                game.setLoyalty(samut, 4)
                val elves = game.findPermanent("Llanowar Elves")!!

                game.execute(
                    ActivateAbility(
                        game.player1Id, samut, minusTwo,
                        targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(elves)),
                        damageDistribution = mapOf(game.player2Id to 1, elves to 1)
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                game.isOnBattlefield("Llanowar Elves") shouldBe false
                game.loyalty(samut) shouldBe 2
            }

            test("−7 puts a creature and a planeswalker card from library onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Samut, the Tested")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Ral, Caller of Storms")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val samut = game.findPermanent("Samut, the Tested")!!
                game.setLoyalty(samut, 7)
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                val ral = game.findCardsInLibrary(1, "Ral, Caller of Storms").single()
                val forest = game.findCardsInLibrary(1, "Forest").single()

                game.execute(ActivateAbility(game.player1Id, samut, minusSeven)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only creature and planeswalker cards are offered") {
                    decision.options.contains(forest) shouldBe false
                }
                game.selectCards(listOf(bears, ral)).error shouldBe null
                game.resolveStack()

                game.state.getBattlefield(game.player1Id).contains(bears) shouldBe true
                game.state.getBattlefield(game.player1Id).contains(ral) shouldBe true
                withClue("the planeswalker enters with its printed loyalty") {
                    game.loyalty(ral) shouldBe 4
                }
                game.state.getLibrary(game.player1Id) shouldBe listOf(forest)
            }
        }
    }
}
