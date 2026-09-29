package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CastRecordComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ColorlessManaSpentTest : ScenarioTestBase() {
    init {
        val condition = Conditions.ManaSpentToCastIncludes(requiredColorless = 1)
        val evaluator = PredicateEvaluator(cardRegistry = null).conditions
        val player = EntityId.generate()
        val source = EntityId.generate()
        fun context(record: CastRecordComponent? = null, triggering: EntityId = source) = EffectContext(
            sourceId = source, controllerId = player,
            triggerContext = TriggerContext(triggeringEntityId = triggering, selfCastManaSpent = record)
        )
        fun state(spell: SpellOnStackComponent? = null, record: CastRecordComponent? = null): GameState {
            var container = ComponentContainer()
            if (spell != null) container = container.with(spell)
            if (record != null) container = container.with(record)
            return GameState().withEntity(source, container)
        }

        test("colorless thresholds and colored requirements are independent on stack and permanent") {
            for (spell in listOf(false, true)) {
                val s = if (spell) state(spell = SpellOnStackComponent(player,
                    manaSpentGreen = 1, manaSpentColorless = 2))
                else state(record = CastRecordComponent(greenSpent = 1, colorlessSpent = 2))
                evaluator.evaluate(s, condition, context()) shouldBe true
                evaluator.evaluate(s, Conditions.ManaSpentToCastIncludes(requiredColorless = 2,
                    requiredGreen = 1), context()) shouldBe true
                evaluator.evaluate(s, Conditions.ManaSpentToCastIncludes(requiredColorless = 3), context()) shouldBe false
                evaluator.evaluate(s, Conditions.ManaSpentToCastIncludes(requiredBlue = 1), context()) shouldBe false
            }
        }
        test("colored payment of generic costs and objects with no payment do not qualify") {
            for (s in listOf(GameState(), state(), state(spell = SpellOnStackComponent(player)),
                state(record = CastRecordComponent(greenSpent = 4)))) {
                evaluator.evaluate(s, condition, context()) shouldBe false
                evaluator.evaluate(s, Conditions.Not(condition), context()) shouldBe true
            }
        }
        test("self-cast payment survives source removal and overrides a subsequent cast") {
            val paid = CastRecordComponent(colorlessSpent = 2, greenSpent = 1)
            val free = CastRecordComponent()
            evaluator.evaluate(GameState(), condition, context(paid)) shouldBe true
            evaluator.evaluate(state(spell = SpellOnStackComponent(player, manaSpentColorless = 3)),
                condition, context(free)) shouldBe false
            evaluator.evaluate(state(spell = SpellOnStackComponent(player)), condition, context(paid)) shouldBe true
            evaluator.evaluate(GameState(), Conditions.ManaSpentToCastIncludes(requiredGreen = 1), context(paid)) shouldBe true
        }
        test("another source does not inherit the triggering spell's payment") {
            evaluator.evaluate(GameState(), condition,
                context(CastRecordComponent(colorlessSpent = 1), EntityId.generate())) shouldBe false
        }
        test("trigger payment snapshot serializes null zero and nonzero separately") {
            for (record in listOf(null, CastRecordComponent(), CastRecordComponent(colorlessSpent = 2, redSpent = 1))) {
                val original = context(record).triggerContext!!
                Json.decodeFromString<TriggerContext>(Json.encodeToString(original)) shouldBe original
            }
        }

        val name = "Colorless Cast Reward"
        cardRegistry.register(card(name) {
            manaCost = "{2}{G}"
            typeLine = "Creature — Bear"
            power = 2
            toughness = 2
            triggeredAbility {
                trigger = Triggers.self.isCast()
                interveningIf = condition
                effect = Effects.GainLife(3)
            }
        })
        for (colorless in listOf(false, true)) {
            test("automatic payment fires cast trigger only for actual colorless mana - $colorless") {
                val game = scenario().withPlayers("Player1", "Player2").withCardInHand(1, name)
                    .withLandsOnBattlefield(1, "Forest", if (colorless) 2 else 3)
                    .withLandsOnBattlefield(1, "Snow-Covered Wastes", if (colorless) 1 else 0)
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.castSpell(1, name).error shouldBe null
                game.state.stack.size shouldBe if (colorless) 2 else 1
                game.resolveStack()
                game.getLifeTotal(1) shouldBe if (colorless) 23 else 20
            }
        }
        for (restricted in listOf(false, true)) {
            test("pooled colorless payment is captured including restricted mana - $restricted") {
                val game = scenario().withPlayers("Player1", "Player2").withCardInHand(1, name)
                    .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val pool = if (restricted) ManaPoolComponent(green = 1,
                    restrictedMana = List(2) { RestrictedManaEntry(null,
                        ManaRestriction.SpellsWithManaValueAtLeast(3)) })
                else ManaPoolComponent(green = 1, colorless = 2)
                game.state = game.state.withEntity(game.player1Id, game.state.getEntity(game.player1Id)!!.with(pool))
                game.castSpell(1, name).error shouldBe null
                game.state.stack.size shouldBe 2
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 23
            }
        }
        test("positive cast trigger still resolves after the paid spell is countered") {
            val game = scenario().withPlayers("Player1", "Player2").withCardInHand(1, name)
                .withCardInHand(1, "Counterspell")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Snow-Covered Wastes", 1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, name).error shouldBe null
            game.state.stack.size shouldBe 2
            game.castSpellTargetingStackSpell(1, "Counterspell", name).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, name) shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
