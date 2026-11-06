package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.kld.cards.KeyToTheCity
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Key to the City (KLD #220)
 * "{T}, Discard a card: Up to one target creature can't be blocked this turn.
 *  Whenever this artifact becomes untapped, you may pay {2}. If you do, draw a card."
 *
 * The untap payoff is a self `becomesUntapped()` trigger feeding a mana may-pay — the trigger fires in the
 * untap step and goes on the stack at upkeep, where its controller answers the pay prompt.
 */
class KeyToTheCityScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = KeyToTheCity.activatedAbilities.first().id

        test("{T}, discard a card: the target creature can't be blocked this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Key to the City")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val key = game.findPermanent("Key to the City")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val fodder = game.state.getHand(game.player1Id).single()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = key,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    costPayment = AdditionalCostPayment(discardedCards = listOf(fodder)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("the Key tapped and the discarded card is in the graveyard") {
                game.state.getEntity(key)?.has<TappedComponent>() shouldBe true
                game.state.getZone(game.player1Id, Zone.GRAVEYARD).contains(fodder) shouldBe true
            }
            withClue("Grizzly Bears can't be blocked") {
                game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }
        }

        test("untapping offers to pay {2}; paying draws a card") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Key to the City", tapped = true)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            withClue("the untap trigger offers the may-pay {2}") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(true)
            game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.handSize(1) shouldBe 1
        }

        test("declining the payment draws nothing") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Key to the City", tapped = true)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            withClue("the untap trigger offers the may-pay {2}") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(false)
            game.resolveStack()

            game.handSize(1) shouldBe 0
        }
    }
}
