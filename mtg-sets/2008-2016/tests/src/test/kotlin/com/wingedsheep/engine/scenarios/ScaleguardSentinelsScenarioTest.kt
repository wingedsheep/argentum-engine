package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scaleguard Sentinels — {G}{G} Creature — Human Soldier 2/3 (DTK #201).
 *
 * "As an additional cost to cast this spell, you may reveal a Dragon card from your hand. This
 * creature enters with a +1/+1 counter on it if you revealed a Dragon card or controlled a Dragon as
 * you cast this spell."
 *
 * The enters-with replacement reads the cast-time capture off the permanent as it enters; revealing
 * a Dragon *and* controlling one still gives just one counter (per the rulings).
 */
class ScaleguardSentinelsScenarioTest : ScenarioTestBase() {

    private fun board(dragonOnBattlefield: Boolean = false, dragonInHand: Boolean = false): TestGame {
        var b = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Scaleguard Sentinels")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (dragonOnBattlefield) b = b.withCardOnBattlefield(1, "Shivan Dragon")
        if (dragonInHand) b = b.withCardInHand(1, "Shivan Dragon")
        return b.build()
    }

    private fun TestGame.sentinelCounters(): Int {
        val sentinels = findPermanent("Scaleguard Sentinels").shouldNotBeNull()
        return state.getEntity(sentinels)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun TestGame.castRevealing() = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Scaleguard Sentinels").single(),
            declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
            additionalCostPayment = AdditionalCostPayment(revealedCards = findCardsInHand(1, "Shivan Dragon")),
        )
    )

    init {
        test("controlling a Dragon as you cast it: enters with a +1/+1 counter") {
            val game = board(dragonOnBattlefield = true)
            game.castSpell(1, "Scaleguard Sentinels").error shouldBe null
            game.resolveStack()

            game.sentinelCounters() shouldBe 1
            game.state.projectedState.getPower(game.findPermanent("Scaleguard Sentinels")!!) shouldBe 3
        }

        test("revealing a Dragon card: enters with a +1/+1 counter") {
            val game = board(dragonInHand = true)
            game.castRevealing().error shouldBe null
            game.resolveStack()

            game.sentinelCounters() shouldBe 1
            game.isInHand(1, "Shivan Dragon") shouldBe true
        }

        test("revealing and controlling a Dragon still gives one counter") {
            val game = board(dragonOnBattlefield = true, dragonInHand = true)
            game.castRevealing().error shouldBe null
            game.resolveStack()

            game.sentinelCounters() shouldBe 1
        }

        test("no Dragon: enters as a plain 2/3") {
            val game = board(dragonInHand = true)
            game.castSpell(1, "Scaleguard Sentinels").error shouldBe null
            game.resolveStack()

            game.sentinelCounters() shouldBe 0
        }
    }
}
