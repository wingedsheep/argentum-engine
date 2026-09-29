package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PlayerCounterPayCostTest : FunSpec({
    val toll = card("Counter Toll") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{X}")
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.PayPlayerCounters(CounterType.ENERGY, 2),
                suffer = Effects.GainLife(DynamicAmount.XValue),
                player = EffectTarget.PlayerRef(Player.AnOpponent),
            )
        }
    }

    for ((energy, answer) in listOf(3 to true, 3 to false, 1 to false)) {
        test("routed energy payment with $energy counters and answer $answer preserves the original effect context") {
            val game = GameTestDriver()
            game.registerCards(TestCards.all + toll)
            game.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
            game.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val controller = game.activePlayer!!
            val payer = game.getOpponent(controller)
            val source = game.putPermanentOnBattlefield(controller, toll.name)
            game.replaceState(game.state.updateEntity(payer) {
                it.with(CountersComponent(mapOf(CounterType.ENERGY to energy)))
            })
            game.giveColorlessMana(controller, 3)
            game.submit(ActivateAbility(controller, source, toll.activatedAbilities.single().id, xValue = 3))
                .error shouldBe null
            game.bothPass()

            if (energy >= 2) {
                val question = game.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                question.playerId shouldBe payer
                val result = game.submitDecision(payer, YesNoResponse(question.id, answer))
                result.error shouldBe null
                val removals = result.events.filterIsInstance<CountersRemovedEvent>()
                if (answer) {
                    removals.single().entityId shouldBe payer
                    removals.single().amount shouldBe 2
                } else removals shouldBe emptyList()
            }

            game.pendingDecision shouldBe null
            val paid = energy >= 2 && answer
            game.getLifeTotal(controller) shouldBe if (paid) 20 else 23
            game.getLifeTotal(payer) shouldBe 20
            game.state.getEntity(payer)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe
                if (paid) energy - 2 else energy
            game.state.getEntity(controller)!!.get<CountersComponent>() shouldBe null
        }
    }
})
