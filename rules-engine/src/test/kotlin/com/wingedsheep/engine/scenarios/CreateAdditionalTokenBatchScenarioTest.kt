package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.ModifyTokenCount
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithAttachedCopy
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Regression net for the token-creation replacement batch (CR 614 — "those tokens plus an
 * additional Food token are created instead" applies once per token-creation event, regardless
 * of how many tokens the event makes; CR 614.5 — the added token is created directly and is
 * self-limiting, so it doesn't re-trigger the replacement).
 *
 * Guards the CreateTokenExecutor fix where applyAdditionalTokenReplacements was invoked twice for
 * a single custom CreateTokenEffect batch, so a multi-token creation added the extra Food *twice*.
 * Predefined-token creations (Food/Treasure/Map) route through CreatePredefinedTokenExecutor and
 * were unaffected — which is why a single-Treasure path like Worldwalker Helm stayed green even
 * with the bug. Only a custom multi-token CreateTokenEffect surfaces the regression, so that is
 * exactly what this exercises, with inline cards so it has no set dependency.
 */
class CreateAdditionalTokenBatchScenarioTest : ScenarioTestBase() {

    // "If one or more tokens would be created under your control, those tokens plus an additional
    // Food token are created instead." (Peregrin Took's replacement clause, on its own.)
    private val additionalFoodMaker = card("Additional Food Maker") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Halfling"
        power = 2
        toughness = 3
        replacementEffect(CreateAdditionalToken(additionalTokenType = "Food"))
    }

    // "Create two 1/1 Soldier creature tokens." A custom CreateTokenEffect batch of two — the exact
    // shape that double-added under the bug. (Rally at the Hornburg, simplified.)
    private val makeTwoSoldiers = card("Make Two Soldiers") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"), count = 2) }
    }

    init {
        cardRegistry.register(additionalFoodMaker)
        cardRegistry.register(makeTwoSoldiers)

        val doubler = card("Batch Doubler") {
            typeLine = "Artifact"
            replacementEffect(MultiplyTokenCreation())
        }
        val modifier = card("Batch Modifier") {
            typeLine = "Artifact"
            replacementEffect(ModifyTokenCount(1))
        }
        val mirror = card("Batch Copy Mirror") {
            typeLine = "Artifact"
            replacementEffect(ReplaceTokenCreationWithAttachedCopy())
        }
        cardRegistry.register(listOf(doubler, modifier, mirror))

        fun robot() = Effects.CreateToken(power = 3, toughness = 3, creatureTypes = setOf("Robot"),
            artifactToken = true, imageUri = "robot-art", initialCounters = mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2))

        test("joint doubling preserves member counters, art and tapped state; modifiers run before doubling") {
            for (withModifier in listOf(false, true)) {
                val builder = scenario().withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Batch Doubler")
                    .withCardOnBattlefield(1, "Additional Food Maker")
                if (withModifier) builder.withCardOnBattlefield(1, "Batch Modifier")
                val game = builder.build()
                val result = services.effectExecutorRegistry.execute(game.state,
                    Effects.CreateTokensTogether(robot(), Effects.CreateTreasure(tapped = true, imageUri = "treasure-art")),
                    EffectContext(null, game.player1Id))
                result.outcome shouldBe Outcome.Done
                game.state = result.state
                val robots = game.findPermanents("Robot Token")
                robots.size shouldBe if (withModifier) 4 else 2
                game.findPermanents("Treasure").size shouldBe 2
                game.findPermanents("Food").size shouldBe 1
                for (id in robots) {
                    game.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                    game.state.getEntity(id)!!.get<CardComponent>()!!.imageUri shouldBe "robot-art"
                }
                for (id in game.findPermanents("Treasure")) {
                    game.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
                    game.state.getEntity(id)!!.get<CardComponent>()!!.imageUri shouldBe "treasure-art"
                }
            }
        }
        test("empty and zero-count joint events add no Food, and later ordinary events retain their own scope") {
            val game = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Additional Food Maker").build()
            val ctx = EffectContext(null, game.player1Id)
            for (empty in listOf(Effects.CreateTokensTogether(), Effects.CreateTokensTogether(Effects.CreateTreasure(count = 0)))) {
                val result = services.effectExecutorRegistry.execute(game.state, empty, ctx)
                result.outcome shouldBe Outcome.Done
                game.state = result.state
            }
            game.findPermanents("Food").size shouldBe 0
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.CreateTokensTogether(robot(), Effects.CreateTreasure()) then Effects.CreateTreasure(), ctx).state
            game.findPermanents("Food").size shouldBe 2
            game.findPermanents("Treasure").size shouldBe 2
        }
        test("all dynamic token stats read the pre-event board and original controller") {
            val game = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Additional Food Maker").build()
            val dynamic = Effects.CreateToken(power = 0, toughness = 4, creatureTypes = setOf("Variable"),
                dynamicPower = DynamicAmounts.creaturesYouControl(), dynamicToughness = DynamicAmounts.creaturesYouControl())
            val result = services.effectExecutorRegistry.execute(game.state,
                Effects.CreateTokensTogether(robot().copy(count = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(2)), dynamic, controller = EffectTarget.SpecificEntity(game.player2Id)),
                EffectContext(null, game.player1Id))
            result.outcome shouldBe Outcome.Done
            game.state = result.state
            val variable = game.findPermanent("Variable Token")!!
            game.state.projectedState.getPower(variable) shouldBe 1
            game.state.projectedState.getToughness(variable) shouldBe 1
            game.state.projectedState.getController(variable) shouldBe game.player2Id
        }
        test("a joint copy decision resumes both accept and decline; new replacement sources cannot modify their own creation") {
            for (accept in listOf(false, true)) {
                val game = scenario().withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Additional Food Maker")
                    .withCardOnBattlefield(1, "Batch Copy Mirror").build()
                val crown = game.findPermanent("Batch Copy Mirror")!!
                val target = game.findPermanent("Additional Food Maker")!!
                game.state = game.state.withEntity(crown, game.state.getEntity(crown)!!.with(AttachedToComponent(target)))
                val ctx = EffectContext(null, game.player1Id)
                val result = services.effectExecutorRegistry.execute(game.state,
                    Effects.CreateTokensTogether(robot(), Effects.CreateTreasure()), ctx)
                result.outcome::class shouldBe Outcome.Paused::class
                game.state = result.state
                game.submitDecision(YesNoResponse(game.state.pendingDecision!!.id, accept)).outcome shouldBe Outcome.Done
                game.findPermanents("Food").size shouldBe 1
                game.findPermanents("Additional Food Maker").size shouldBe if (accept) 3 else 1
                game.findPermanents("Robot Token").size shouldBe if (accept) 0 else 1
                game.findPermanents("Treasure").size shouldBe if (accept) 0 else 1
                game.state = services.effectExecutorRegistry.execute(game.state, Effects.CreateTreasure(), ctx).state
                game.findPermanents("Food").size shouldBe if (accept) 4 else 2
            }
        }

        context("additional-token replacement on a custom multi-token batch") {

            test("creating two tokens adds exactly one Food — the replacement fires once per event, not per token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Additional Food Maker")
                    .withCardInHand(1, "Make Two Soldiers")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Make Two Soldiers")
                game.resolveStack()

                withClue("Both primary Soldier tokens are created") {
                    game.findPermanents("Soldier Token").size shouldBe 2
                }
                withClue("Exactly one additional Food — not two (the pre-fix bug double-added per batch)") {
                    game.findPermanents("Food").size shouldBe 1
                }
            }
        }
    }
}
