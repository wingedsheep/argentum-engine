package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.matchers.shouldBe

/**
 * Sprout Swarm (FUT #138) — {1}{G} Instant. Convoke. Buyback {3}. Create a 1/1 green Saproling
 * creature token.
 *
 * Convoke can help pay the buyback cost too (it pays for the total cost, CR 702.51a), and a
 * bought-back Swarm comes back to hand to be cast again.
 */
class SproutSwarmScenarioTest : ScenarioTestBase() {

    init {
        test("convoked with buyback, it makes a Saproling and returns to hand") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Sprout Swarm")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bear = game.findPermanent("Grizzly Bears")!!

            // {1}{G} + buyback {3} = five mana: four Forests plus the convoked Bears.
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Sprout Swarm").first(),
                    declaredCostSlot = ChoiceSlot.BOUGHT_BACK,
                    alternativePayment = AlternativePaymentChoice(
                        convokedCreatures = mapOf(bear to ConvokePayment(color = Color.GREEN))
                    ),
                )
            ).error shouldBe null
            game.resolveStack()

            game.findPermanents("Saproling Token").size shouldBe 1
            game.isInHand(1, "Sprout Swarm") shouldBe true
            game.isInGraveyard(1, "Sprout Swarm") shouldBe false
            game.state.getEntity(bear)?.has<TappedComponent>() shouldBe true
        }

        test("cast without buyback, it goes to the graveyard") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Sprout Swarm")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sprout Swarm").error shouldBe null
            game.resolveStack()

            game.findPermanents("Saproling Token").size shouldBe 1
            game.isInGraveyard(1, "Sprout Swarm") shouldBe true
        }
    }
}
