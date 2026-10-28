package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.MayCastFromGraveyardUsedThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Chainer, Nightmare Adept (C19 #39).
 *
 * Discard a card: You may cast a creature spell from your graveyard this turn. Activate only once
 * each turn.
 * Whenever a nontoken creature you control enters, if you didn't cast it from your hand, it gains
 * haste until your next turn.
 */
class ChainerNightmareAdeptScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.chainerBoard() = this
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Chainer, Nightmare Adept")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withLandsOnBattlefield(1, "Forest", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.payAndResolve() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    private fun TestGame.activateChainer(discard: String) {
        val chainer = findPermanent("Chainer, Nightmare Adept")!!
        val abilityId = cardRegistry.getCard("Chainer, Nightmare Adept")!!.script.activatedAbilities[0].id
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = chainer,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(findCardsInHand(1, discard).first()))
            )
        ).error shouldBe null
        resolveStack()
    }

    init {
        context("Chainer, Nightmare Adept — haste trigger") {

            test("a creature cast from your hand does not gain haste") {
                val game = scenario().chainerBoard().withCardInHand(1, "Grizzly Bears").build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.payAndResolve()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("cast from hand: the intervening-if fails") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }
            }

            test("a creature returned from the graveyard (not cast) gains haste") {
                val game = scenario().chainerBoard()
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Zombify")
                    .build()

                val bearsCard = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(bearsCard)).error shouldBe null
                game.payAndResolve()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
            }
        }

        context("Chainer, Nightmare Adept — graveyard permission") {

            test("discarding lets you cast one creature spell from your graveyard, and it gains haste") {
                val game = scenario().chainerBoard()
                    .withCardInHand(1, "Forest")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Walking Corpse")
                    .build()

                withClue("no permission before activating") {
                    game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldNotBe null
                }

                game.activateChainer(discard = "Forest")
                game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldBe null
                game.payAndResolve()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("cast from the graveyard, not the hand: it gains haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
                withClue("the permission covered one spell; a second creature can't be cast") {
                    game.castSpellFromGraveyard(1, "Walking Corpse").error shouldNotBe null
                }
            }

            test("a cast through Chainer's permission spends it, not another grant's once-per-turn allowance") {
                val game = scenario().chainerBoard()
                    .withCardOnBattlefield(1, "Kotis, Sibsig Champion")
                    .withCardInHand(1, "Forest")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .build()

                game.activateChainer(discard = "Forest")
                game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldBe null
                game.payAndResolve()

                withClue("the free one-shot permission paid for the cast") {
                    game.state.grantedStaticAbilities.shouldBeEmpty()
                }
                withClue("Kotis's exile-three allowance is still unused this turn") {
                    val kotis = game.findPermanent("Kotis, Sibsig Champion")!!
                    game.state.getEntity(kotis)!!.has<MayCastFromGraveyardUsedThisTurnComponent>() shouldBe false
                }
            }

            test("an unused permission lapses at end of turn") {
                val game = scenario().chainerBoard()
                    .withCardInHand(1, "Forest")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .build()

                game.activateChainer(discard = "Forest")
                game.state.grantedStaticAbilities.size shouldBe 1

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.grantedStaticAbilities.shouldBeEmpty()
            }
        }
    }
}
