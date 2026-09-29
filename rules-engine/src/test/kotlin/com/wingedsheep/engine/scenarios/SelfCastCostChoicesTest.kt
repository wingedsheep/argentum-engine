package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class SelfCastCostChoicesTest : ScenarioTestBase() {
    init {
        for (slot in listOf(ChoiceSlot.KICKED, ChoiceSlot.BARGAINED)) {
            val name = "Self Cast ${slot.name}"
            cardRegistry.register(card(name) {
                manaCost = "{1}"
                typeLine = "Creature — Bear"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.OptionalAdditionalCost(
                    manaCost = ManaCost.parse("{1}"), declaredSlot = slot
                ))
                triggeredAbility {
                    trigger = Triggers.self.isCast()
                    interveningIf = Conditions.CastChoiceMade(slot)
                    effect = Effects.GainLife(3)
                }
            })
            for (paid in listOf(false, true)) {
                test("$slot self-cast condition and resolution with declared cost $paid") {
                    val game = scenario().withPlayers("Player1", "Player2").withCardInHand(1, name)
                        .withLandsOnBattlefield(1, "Snow-Covered Wastes", 2)
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                    game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, name).single(),
                        declaredCostSlot = slot.takeIf { paid })).error shouldBe null
                    game.state.stack.size shouldBe if (paid) 2 else 1
                    game.resolveStack()
                    game.getLifeTotal(1) shouldBe if (paid) 23 else 20
                }
            }
        }

        for (mandatory in listOf(false, true)) {
            val name = "Self Cast Modal $mandatory"
            cardRegistry.register(card(name) {
                manaCost = "{1}"
                typeLine = "Creature — Bear"
                power = 2
                toughness = 2
                keywordAbility(KeywordAbility.kicker("{1}"))
                triggeredAbility {
                    trigger = Triggers.self.isCast()
                    val count = DynamicAmounts.conditional(Conditions.WasKicked, 2, 1)
                    effect = Effects.Modal(
                        modes = listOf(
                            mode("Gain 1 life, then discard") {
                                effect = Effects.If(Conditions.WasKicked, Effects.GainLife(1)) then Effects.Discard(1)
                            },
                            mode("Gain 3 life if kicked") {
                                effect = Effects.If(Conditions.WasKicked, Effects.GainLife(3))
                            }
                        ),
                        dynamicChooseCount = count,
                        dynamicMinChooseCount = count.takeIf { mandatory }
                    )
                }
            })
            test("self-cast modal uses dynamic minimum when supplied - $mandatory") {
                val game = scenario().withPlayers("Player1", "Player2").withCardInHand(1, name)
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Snow-Covered Wastes", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, name).single(),
                    declaredCostSlot = ChoiceSlot.KICKED)).error shouldBe null
                val first = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                first.options.size shouldBe if (mandatory) 2 else 3
                game.submitDecision(OptionChosenResponse(first.id, 0)).error shouldBe null
                val second = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                second.options.size shouldBe if (mandatory) 1 else 2
                game.submitDecision(OptionChosenResponse(second.id, if (mandatory) 0 else 1)).error shouldBe null
                game.resolveStack()
                game.selectCards(game.findCardsInHand(1, "Forest").take(1)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe if (mandatory) 24 else 21
            }
        }

        val evaluator = PredicateEvaluator(cardRegistry = null).conditions
        val source = EntityId.generate()
        val player = EntityId.generate()
        fun context(slots: Set<ChoiceSlot>, triggering: EntityId = source) = EffectContext(
            sourceId = source, controllerId = player,
            triggerContext = TriggerContext(triggeringEntityId = triggering,
                selfCastCostChoices = listOf(ChoiceSlot.KICKED, ChoiceSlot.BARGAINED).associateWith { it in slots })
        )
        test("snapshot remains true without a source and bargain never reads as kicker") {
            val state = GameState()
            evaluator.evaluate(state, Conditions.WasKicked, context(setOf(ChoiceSlot.KICKED))) shouldBe true
            evaluator.evaluate(state, Conditions.WasKicked, context(setOf(ChoiceSlot.BARGAINED))) shouldBe false
            evaluator.evaluate(state, Conditions.WasBargained, context(setOf(ChoiceSlot.BARGAINED))) shouldBe true
        }
        test("empty snapshot overrides a later cast of the same card and preserves the original declaration") {
            val state = GameState().withEntity(source, ComponentContainer().with(CastChoicesComponent(
                chosen = mapOf(ChoiceSlot.KICKED to ChoiceValue.Flag)
            )))
            evaluator.evaluate(state, Conditions.WasKicked, context(emptySet())) shouldBe false
            evaluator.evaluate(state, Conditions.CastChoiceMade(ChoiceSlot.KICKED), context(emptySet())) shouldBe false
            evaluator.evaluate(state, Conditions.WasBargained, context(setOf(ChoiceSlot.BARGAINED))) shouldBe true
            evaluator.evaluate(state, Conditions.WasKicked, context(setOf(ChoiceSlot.BARGAINED))) shouldBe false
        }
        test("a different source cannot inherit the triggering spell's declared costs") {
            val ctx = context(setOf(ChoiceSlot.KICKED), EntityId.generate())
            evaluator.evaluate(GameState(), Conditions.WasKicked, ctx) shouldBe false
            evaluator.evaluate(GameState(), Conditions.CastChoiceMade(ChoiceSlot.KICKED), ctx) shouldBe false
        }
        test("cost snapshots do not mask unrelated durable choice slots") {
            val state = GameState().withEntity(source, ComponentContainer().with(CastChoicesComponent(
                chosen = mapOf(ChoiceSlot.COLOR to ChoiceValue.ColorChoice(com.wingedsheep.sdk.core.Color.BLUE))
            )))
            evaluator.evaluate(state, Conditions.CastChoiceMade(ChoiceSlot.COLOR),
                context(setOf(ChoiceSlot.KICKED))) shouldBe true
        }
        test("trigger serialization distinguishes no snapshot from an unkicked snapshot") {
            for (slots in listOf(null, emptySet(), setOf(ChoiceSlot.KICKED), setOf(ChoiceSlot.BARGAINED))) {
                val original = TriggerContext(triggeringEntityId = source,
                    selfCastCostChoices = slots?.associateWith { true })
                Json.decodeFromString<TriggerContext>(Json.encodeToString(original)) shouldBe original
            }
        }
    }
}
