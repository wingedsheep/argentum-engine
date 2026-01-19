package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.reflect.KClass

class EffectHandlerRegistryTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    context("default registry") {
        test("contains all standard handlers") {
            val registry = EffectHandlerRegistry.default()

            registry.hasHandler(GainLifeEffect::class) shouldBe true
            registry.hasHandler(LoseLifeEffect::class) shouldBe true
            registry.hasHandler(DealDamageEffect::class) shouldBe true
            registry.hasHandler(DrawCardsEffect::class) shouldBe true
            registry.hasHandler(DiscardCardsEffect::class) shouldBe true
            registry.hasHandler(DestroyEffect::class) shouldBe true
            registry.hasHandler(ExileEffect::class) shouldBe true
            registry.hasHandler(ReturnToHandEffect::class) shouldBe true
            registry.hasHandler(TapUntapEffect::class) shouldBe true
            registry.hasHandler(ModifyStatsEffect::class) shouldBe true
            registry.hasHandler(AddCountersEffect::class) shouldBe true
            registry.hasHandler(AddManaEffect::class) shouldBe true
            registry.hasHandler(AddColorlessManaEffect::class) shouldBe true
            registry.hasHandler(CreateTokenEffect::class) shouldBe true
            registry.hasHandler(CompositeEffect::class) shouldBe true
            registry.hasHandler(ConditionalEffect::class) shouldBe true
            registry.hasHandler(ShuffleIntoLibraryEffect::class) shouldBe true
            registry.hasHandler(LookAtTopCardsEffect::class) shouldBe true
            registry.hasHandler(MustBeBlockedEffect::class) shouldBe true
            registry.hasHandler(GrantKeywordUntilEndOfTurnEffect::class) shouldBe true
            registry.hasHandler(DestroyAllLandsEffect::class) shouldBe true
            registry.hasHandler(DestroyAllCreaturesEffect::class) shouldBe true
        }

        test("executes GainLifeEffect correctly") {
            val registry = EffectHandlerRegistry.default()
            val state = newGame()
            val effect = GainLifeEffect(5, EffectTarget.Controller)
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            result.state.getEntity(player1Id)?.get<com.wingedsheep.rulesengine.ecs.components.LifeComponent>()?.life shouldBe 25
        }
    }

    context("builder pattern") {
        test("creates empty registry") {
            val registry = EffectHandlerRegistry.builder().build()

            registry.size shouldBe 0
            registry.hasHandler(GainLifeEffect::class) shouldBe false
        }

        test("registers single handler") {
            val registry = EffectHandlerRegistry.builder()
                .register(GainLifeHandler())
                .build()

            registry.size shouldBe 1
            registry.hasHandler(GainLifeEffect::class) shouldBe true
            registry.hasHandler(LoseLifeEffect::class) shouldBe false
        }

        test("registers multiple handlers with registerAll") {
            val registry = EffectHandlerRegistry.builder()
                .registerAll(
                    GainLifeHandler(),
                    LoseLifeHandler(),
                    DealDamageHandler()
                )
                .build()

            registry.size shouldBe 3
            registry.hasHandler(GainLifeEffect::class) shouldBe true
            registry.hasHandler(LoseLifeEffect::class) shouldBe true
            registry.hasHandler(DealDamageEffect::class) shouldBe true
        }

        test("chained registration") {
            val registry = EffectHandlerRegistry.builder()
                .register(GainLifeHandler())
                .register(LoseLifeHandler())
                .register(DrawCardsHandler())
                .build()

            registry.size shouldBe 3
        }
    }

    context("error handling") {
        test("throws for unregistered effect type") {
            val registry = EffectHandlerRegistry.builder()
                .register(GainLifeHandler())
                .build()

            val state = newGame()
            val effect = LoseLifeEffect(3, EffectTarget.Opponent)  // Not registered
            val context = ExecutionContext(player1Id, player1Id)

            val exception = shouldThrow<IllegalStateException> {
                registry.execute(state, effect, context)
            }

            exception.message shouldBe "No handler registered for effect type: com.wingedsheep.rulesengine.ability.LoseLifeEffect"
        }
    }

    context("handler replacement") {
        test("later registration replaces earlier one") {
            // Custom handler that doubles the life gain
            class DoubleGainLifeHandler : BaseEffectHandler<GainLifeEffect>() {
                override val effectClass: KClass<GainLifeEffect> = GainLifeEffect::class

                override fun execute(
                    state: GameState,
                    effect: GainLifeEffect,
                    context: ExecutionContext
                ): ExecutionResult {
                    val targetId = when (effect.target) {
                        is EffectTarget.Controller -> context.controllerId
                        is EffectTarget.Opponent -> state.getPlayerIds().first { it != context.controllerId }
                        else -> context.controllerId
                    }
                    val lifeComponent = state.getEntity(targetId)?.get<com.wingedsheep.rulesengine.ecs.components.LifeComponent>()
                        ?: return noOp(state)

                    val newState = state.updateEntity(targetId) { c ->
                        c.with(lifeComponent.gainLife(effect.amount * 2))  // Double the gain
                    }

                    return ExecutionResult(newState)
                }
            }

            val registry = EffectHandlerRegistry.builder()
                .register(GainLifeHandler())
                .register(DoubleGainLifeHandler())  // Replaces the first handler
                .build()

            val state = newGame()
            val effect = GainLifeEffect(5, EffectTarget.Controller)
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            // Should gain 10 (5 * 2) instead of 5
            result.state.getEntity(player1Id)?.get<com.wingedsheep.rulesengine.ecs.components.LifeComponent>()?.life shouldBe 30
        }
    }

    context("composite effects delegation") {
        test("CompositeEffect recursively calls registry") {
            val registry = EffectHandlerRegistry.default()
            val state = newGame()

            val effect = CompositeEffect(
                listOf(
                    GainLifeEffect(3, EffectTarget.Controller),
                    LoseLifeEffect(2, EffectTarget.Opponent)
                )
            )
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            result.state.getEntity(player1Id)?.get<com.wingedsheep.rulesengine.ecs.components.LifeComponent>()?.life shouldBe 23
            result.state.getEntity(player2Id)?.get<com.wingedsheep.rulesengine.ecs.components.LifeComponent>()?.life shouldBe 18
            result.events.size shouldBe 2
        }

        test("nested composite effects work correctly") {
            val registry = EffectHandlerRegistry.default()
            val state = newGame()

            val innerComposite = CompositeEffect(
                listOf(
                    GainLifeEffect(1, EffectTarget.Controller),
                    GainLifeEffect(1, EffectTarget.Controller)
                )
            )

            val outerComposite = CompositeEffect(
                listOf(
                    innerComposite,
                    GainLifeEffect(1, EffectTarget.Controller)
                )
            )

            val context = ExecutionContext(player1Id, player1Id)
            val result = registry.execute(state, outerComposite, context)

            result.state.getEntity(player1Id)?.get<com.wingedsheep.rulesengine.ecs.components.LifeComponent>()?.life shouldBe 23  // 20 + 1 + 1 + 1
        }
    }
})
