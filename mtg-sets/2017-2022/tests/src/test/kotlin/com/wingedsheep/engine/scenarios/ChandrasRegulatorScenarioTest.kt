package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra's Regulator (M20 #131) — "Whenever you activate a loyalty ability of a Chandra
 * planeswalker, you may pay {1}. If you do, copy that ability. You may choose new targets for the
 * copy. / {1}, {T}, Discard a Mountain card or a red card: Draw a card."
 *
 * Pins that paying {1} really copies the Chandra ability (two pings, loyalty moved only once),
 * that declining leaves the original alone, and the discard filter on the rummage ability.
 */
class ChandrasRegulatorScenarioTest : ScenarioTestBase() {

    private fun plusOneOf(name: String) = cardRegistry.getCard(name)!!.script.activatedAbilities
        .single { (it.cost as? AbilityCost.Loyalty)?.change == 1 }.id

    private val drawAbility = cardRegistry.getCard("Chandra's Regulator")!!.script.activatedAbilities.single().id

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun chandraGame() = scenario().withPlayers()
        .withCardOnBattlefield(1, "Chandra's Regulator")
        .withCardOnBattlefield(1, "Chandra Nalaar")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("paying {1} copies the Chandra loyalty ability without moving loyalty again") {
            val game = chandraGame()
            val chandra = game.findPermanent("Chandra Nalaar")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, chandra, plusOneOf("Chandra Nalaar"),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            val loyaltyAfterActivation = game.loyalty(chandra)

            game.resolveStack()
            withClue("the Regulator trigger asks for the optional {1}") {
                game.hasPendingDecision() shouldBe true
            }
            game.answerYesNo(true)
            game.submitManaSourcesAutoPay()
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(game.player2Id))
            }
            game.resolveStack()

            withClue("the copy and the original each deal 1 damage") {
                game.getLifeTotal(2) shouldBe 18
            }
            withClue("copying a loyalty ability doesn't add or remove loyalty counters") {
                game.loyalty(chandra) shouldBe loyaltyAfterActivation
            }
        }

        test("declining the {1} leaves only the original ability") {
            val game = chandraGame()
            val chandra = game.findPermanent("Chandra Nalaar")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, chandra, plusOneOf("Chandra Nalaar"),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
        }

        test("a non-Chandra planeswalker's loyalty ability doesn't trigger it") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Chandra's Regulator")
                .withCardOnBattlefield(1, "Ajani Goldmane")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ajani = game.findPermanent("Ajani Goldmane")!!
            game.execute(ActivateAbility(game.player1Id, ajani, plusOneOf("Ajani Goldmane"))).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
        }

        test("discarding a Mountain card draws a card; a nonred non-Mountain card can't be discarded") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Chandra's Regulator")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Island")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val regulator = game.findPermanent("Chandra's Regulator")!!
            val island = game.findCardsInHand(1, "Island").single()
            val mountain = game.findCardsInHand(1, "Mountain").single()

            withClue("an Island is neither a Mountain card nor red") {
                game.execute(
                    ActivateAbility(
                        game.player1Id, regulator, drawAbility,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(island)),
                    )
                ).error shouldNotBe null
            }

            game.execute(
                ActivateAbility(
                    game.player1Id, regulator, drawAbility,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(mountain)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Mountain") shouldBe true
            game.isInHand(1, "Plains") shouldBe true
        }
    }
}
