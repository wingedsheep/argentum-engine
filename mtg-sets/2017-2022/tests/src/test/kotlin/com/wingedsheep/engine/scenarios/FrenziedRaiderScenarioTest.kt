package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Frenzied Raider (KHM #137) — "Whenever you activate a boast ability, put a +1/+1 counter on this
 * creature."
 *
 * Covers: a boast activation triggers it, and the trigger resolves before the boast (ruling); an
 * ordinary activated ability and an exhaust ability (another "activate only once" keyword) don't.
 */
class FrenziedRaiderScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            card("Raider Test Skald") {
                manaCost = "{1}"
                typeLine = "Creature — Human Berserker"
                power = 1
                toughness = 1
                activatedAbility {
                    isBoast = true
                    cost = Costs.Free
                    effect = Effects.GainLife(3)
                }
                activatedAbility {
                    cost = Costs.Free
                    effect = Effects.GainLife(1)
                    timing = TimingRule.InstantSpeed
                }
                activatedAbility {
                    isExhaust = true
                    cost = Costs.Free
                    effect = Effects.GainLife(2)
                }
            }
        )

        fun TestGame.counters(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun boardAfterAttack(): TestGame {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Frenzied Raider")
                .withCardOnBattlefield(1, "Raider Test Skald")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Raider Test Skald" to 2)).error shouldBe null
            return game
        }

        val skaldDef by lazy { cardRegistry.getCard("Raider Test Skald")!! }

        test("activating a boast ability puts a counter on Frenzied Raider before the boast resolves") {
            val game = boardAfterAttack()
            val raider = game.findPermanent("Frenzied Raider")!!
            val skald = game.findPermanent("Raider Test Skald")!!
            val life = game.getLifeTotal(1)

            game.execute(
                ActivateAbility(game.player1Id, skald, skaldDef.activatedAbilities.single { it.isBoast }.id)
            ).error shouldBe null
            withClue("the trigger is stacked above the boast") { game.state.stack.size shouldBe 2 }

            game.passPriority()
            game.passPriority()
            withClue("trigger resolved, boast still waiting") {
                game.counters(raider) shouldBe 1
                game.state.stack.size shouldBe 1
                game.getLifeTotal(1) shouldBe life
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe life + 3
            game.counters(raider) shouldBe 1
        }

        test("an ordinary activated ability and an exhaust ability don't trigger it") {
            val game = boardAfterAttack()
            val raider = game.findPermanent("Frenzied Raider")!!
            val skald = game.findPermanent("Raider Test Skald")!!
            val plain = skaldDef.activatedAbilities.single { !it.isBoast && !it.isExhaust }.id
            val exhaust = skaldDef.activatedAbilities.single { it.isExhaust }.id

            game.execute(ActivateAbility(game.player1Id, skald, plain)).error shouldBe null
            game.resolveStack()
            game.execute(ActivateAbility(game.player1Id, skald, exhaust)).error shouldBe null
            game.resolveStack()

            game.counters(raider) shouldBe 0
        }
    }
}
