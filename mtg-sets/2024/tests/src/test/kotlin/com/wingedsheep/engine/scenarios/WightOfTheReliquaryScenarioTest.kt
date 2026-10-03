package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wight of the Reliquary (MH3 #207) — {B}{G} Creature — Zombie Knight 2/2
 *
 *   Vigilance
 *   This creature gets +1/+1 for each creature card in your graveyard.
 *   {T}, Sacrifice another creature: Search your library for a land card, put it onto the
 *   battlefield tapped, then shuffle.
 */
class WightOfTheReliquaryScenarioTest : ScenarioTestBase() {

    init {
        context("Wight of the Reliquary") {
            test("grows with creature cards in your graveyard only, and has vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Wight of the Reliquary")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wight = game.findPermanent("Wight of the Reliquary")!!
                val projected = game.state.projectedState
                withClue("two creature cards in my graveyard; lands and opponent's cards don't count") {
                    projected.getPower(wight) shouldBe 4
                    projected.getToughness(wight) shouldBe 4
                }
                projected.hasKeyword(wight, Keyword.VIGILANCE) shouldBe true
            }

            test("tap + sacrifice another creature fetches any land tapped, and the sacrifice grows it") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Wight of the Reliquary", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wight = game.findPermanent("Wight of the Reliquary")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Wight of the Reliquary")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wight,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                (decision is SelectCardsDecision) shouldBe true
                withClue("only land cards are searchable") {
                    (decision as SelectCardsDecision).options
                        .contains(game.findCardsInLibrary(1, "Grizzly Bears").first()) shouldBe false
                }
                game.selectCards(listOf(game.findCardsInLibrary(1, "Forest").first())).error shouldBe null
                game.resolveStack()

                val forest = game.findPermanent("Forest")
                forest shouldNotBe null
                withClue("the land enters tapped") {
                    game.state.getEntity(forest!!)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the Wight is tapped as part of the cost") {
                    game.state.getEntity(wight)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the sacrificed Bears now count toward the buff") {
                    game.state.projectedState.getPower(wight) shouldBe 3
                }
            }

            test("can't sacrifice itself to pay") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Wight of the Reliquary", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wight = game.findPermanent("Wight of the Reliquary")!!
                val abilityId = cardRegistry.getCard("Wight of the Reliquary")!!.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wight,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(wight))
                    )
                ).error shouldNotBe null
            }
        }
    }
}
