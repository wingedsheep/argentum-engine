package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sun-Blessed Guardian // Furnace-Blessed Conqueror (MOM #38).
 *
 *   Front (2/2) — "{5}{R/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (3/3) — "Whenever this creature attacks, create a tapped and attacking token that's a copy
 *                  of it. Put a +1/+1 counter on that token for each +1/+1 counter on this creature.
 *                  Sacrifice that token at the beginning of the next end step."
 */
class SunBlessedGuardianScenarioTest : ScenarioTestBase() {

    private fun transformedGame(plusOneCounters: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Sun-Blessed Guardian")
            .withLandsOnBattlefield(1, "Plains", 5)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val guardian = game.findPermanent("Sun-Blessed Guardian")!!
        val abilityId = cardRegistry.getCard("Sun-Blessed Guardian")!!.activatedAbilities.first().id
        game.execute(
            ActivateAbility(
                playerId = game.player1Id, sourceId = guardian, abilityId = abilityId,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        game.state.getEntity(guardian)!!.get<CardComponent>()!!.name shouldBe "Furnace-Blessed Conqueror"

        if (plusOneCounters > 0) {
            game.state = game.state.updateEntity(guardian) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent())
                    .withAdded(CounterType.PLUS_ONE_PLUS_ONE, plusOneCounters))
            }
        }
        return game
    }

    init {
        context("Sun-Blessed Guardian") {

            test("transforms for {5}{R} into a 3/3 Furnace-Blessed Conqueror") {
                val game = transformedGame(0)
                val conqueror = game.findPermanent("Furnace-Blessed Conqueror")!!
                game.state.projectedState.getPower(conqueror) shouldBe 3
                game.state.projectedState.getToughness(conqueror) shouldBe 3
                game.getLifeTotal(1) shouldBe 20
            }

            test("attacking makes a tapped, attacking copy with matching +1/+1 counters, sacrificed at end step") {
                val game = transformedGame(2)
                val conqueror = game.findPermanent("Furnace-Blessed Conqueror")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Furnace-Blessed Conqueror" to 2)).error shouldBe null
                game.resolveStack()

                val token = game.findPermanents("Furnace-Blessed Conqueror").single { it != conqueror }
                withClue("the token is a tapped, attacking copy of the back face") {
                    game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
                    game.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
                    game.state.getEntity(token)!!.has<AttackingComponent>() shouldBe true
                }
                withClue("two +1/+1 counters, matching the Conqueror: a 5/5") {
                    game.state.getEntity(token)!!.get<CountersComponent>()!!
                        .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                    game.state.projectedState.getPower(token) shouldBe 5
                    game.state.projectedState.getToughness(token) shouldBe 5
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("the token is sacrificed at the beginning of the next end step") {
                    game.findPermanents("Furnace-Blessed Conqueror") shouldBe listOf(conqueror)
                }
            }

            test("with no counters on the Conqueror the token is a plain 3/3") {
                val game = transformedGame(0)
                val conqueror = game.findPermanent("Furnace-Blessed Conqueror")!!
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Furnace-Blessed Conqueror" to 2)).error shouldBe null
                game.resolveStack()

                val token = game.findPermanents("Furnace-Blessed Conqueror").single { it != conqueror }
                game.state.projectedState.getPower(token) shouldBe 3
                game.state.projectedState.getToughness(token) shouldBe 3
            }
        }
    }
}
