package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TakePlayerAction
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class PlayerActionPermissionTest : ScenarioTestBase() {
    init {
        test("permission belongs to recipient, resolves immediately, and preserves priority") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            val grant = Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Pay 1 life: gain 2 life")
            game.state = services.effectExecutorRegistry.execute(game.state, grant, EffectContext(null, game.player1Id)).state
            val id = game.state.playerActionPermissions.single().id
            game.execute(TakePlayerAction(game.player2Id, id)).error!! shouldContain "another player"
            game.getLifeTotal(2) shouldBe 20
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.getLifeTotal(1) shouldBe 21
            game.state.stack shouldBe emptyList()
            game.state.priorityPlayerId shouldBe game.player1Id
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.getLifeTotal(1) shouldBe 22
        }
        test("granting the same snapshot produces the same permission routing identity") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            val grant = Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action")
            val context = EffectContext(null, game.player1Id)
            services.effectExecutorRegistry.execute(game.state, grant, context) shouldBe
                services.effectExecutorRegistry.execute(game.state, grant, context)
        }
        test("controlled turns retain the affected seat as the permission owner and payer") {
            val game = scenario().withPlayers().withActivePlayer(2).build()
            game.state = game.state.updateEntity(game.player2Id) {
                it.with(com.wingedsheep.engine.state.components.player.HotseatControlComponent(game.player1Id))
            }
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"),
                EffectContext(null, game.player2Id)).state.withPriority(game.player2Id)
            val action = TakePlayerAction(game.player2Id, game.state.playerActionPermissions.single().id)
            services.legalActionEnumerator.enumerate(game.state, game.player2Id).any { it.action == action } shouldBe true
            game.execute(action).error shouldBe null
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 21
        }
        test("only the granted duration expires at cleanup") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            for (duration in listOf(Duration.EndOfTurn, Duration.Permanent)) {
                game.state = services.effectExecutorRegistry.execute(game.state,
                    Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action", duration = duration),
                    EffectContext(null, game.player1Id)).state
            }
            val expiredId = game.state.playerActionPermissions.first().id
            game.state = services.turnManager.cleanupPhaseManager.cleanupEndOfTurn(game.state)
            game.state.playerActionPermissions.map { it.action.duration } shouldBe listOf(Duration.Permanent)
            game.execute(TakePlayerAction(game.player1Id, expiredId)).error!! shouldContain "no longer exists"
        }
        test("sorcery permissions are unavailable on opponents turns") {
            val game = scenario().withPlayers().withActivePlayer(2).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Sorcery, "Sorcery action"),
                EffectContext(null, game.player1Id)).state.withPriority(game.player1Id)
            val action = TakePlayerAction(game.player1Id, game.state.playerActionPermissions.single().id)
            game.execute(action).error!! shouldContain "sorcery timing"
            services.legalActionEnumerator.enumerate(game.state, game.player1Id).none { it.action is TakePlayerAction } shouldBe true
        }
        test("split second permits the special action and does not report an ability activation") {
            cardRegistry.register(com.wingedsheep.sdk.dsl.card("Split-second test") {
                manaCost = "{0}"; typeLine = "Instant"
                keywords(com.wingedsheep.sdk.core.Keyword.SPLIT_SECOND)
                spell { effect = Effects.GainLife(1) }
            })
            val game = scenario().withPlayers().withCardInHand(2, "Split-second test").withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"), EffectContext(null, game.player1Id)).state
            val action = TakePlayerAction(game.player1Id, game.state.playerActionPermissions.single().id)
            game.state = game.state.withPriority(game.player2Id)
            game.castSpell(2, "Split-second test").error shouldBe null
            game.passPriority().error shouldBe null
            services.legalActionEnumerator.enumerate(game.state, game.player1Id).any { it.action == action } shouldBe true
            val result = game.execute(action)
            result.error shouldBe null
            result.events.none { it is com.wingedsheep.engine.core.AbilityActivatedEvent } shouldBe true
            game.state.stack.size shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("serialized permission retains targets and its captured context") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            val context = EffectContext(null, game.player1Id, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id)), xValue = 7)
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2, com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0)), PlayerActionTiming.Instant, "Life action"), context).state
            val permission = game.state.playerActionPermissions.single()
            val restored = kotlinx.serialization.json.Json.decodeFromString<com.wingedsheep.engine.event.PlayerActionPermission>(
                kotlinx.serialization.json.Json.encodeToString(com.wingedsheep.engine.event.PlayerActionPermission.serializer(), permission))
            restored shouldBe permission
            game.state = game.state.copy(playerActionPermissions = listOf(restored))
            game.execute(TakePlayerAction(game.player1Id, restored.id)).error shouldBe null
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 22
        }
        test("instant permissions are withheld during a resolution mana payment") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Mountain", 1).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"), EffectContext(null, game.player1Id)).state
            val permission = game.state.playerActionPermissions.single()
            val payment = services.costPaymentService.pay(game.state, game.player1Id, Costs.pay.Mana("{1}"), game.player1Id)
            game.state = payment.state
            game.answerYesNo(true).error shouldBe null
            (game.state.pendingDecision is com.wingedsheep.engine.core.SelectManaSourcesDecision) shouldBe true
            game.execute(TakePlayerAction(game.player1Id, permission.id)).error!! shouldContain "decision"
            services.legalActionEnumerator.enumerateManaAbilities(game.state, game.player1Id).none { it.action is TakePlayerAction } shouldBe true
        }
        test("no special action interrupts declaring attackers") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"), EffectContext(null, game.player1Id)).state
            game.advanceToPhase(com.wingedsheep.sdk.core.Phase.COMBAT, com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS)
            val permission = game.state.playerActionPermissions.single()
            game.execute(TakePlayerAction(game.player1Id, permission.id)).error!! shouldContain "priority"
            services.legalActionEnumerator.enumerate(game.state, game.player1Id).none { it.action is TakePlayerAction } shouldBe true
        }

        test("unaffordable life payments reject atomically") {
            val game = scenario().withPlayers().withLifeTotal(1, 1).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(2), Effects.GainLife(5), PlayerActionTiming.Instant, "Pay 2 life"),
                EffectContext(null, game.player1Id)).state
            val before = game.state
            val result = game.execute(TakePlayerAction(game.player1Id, game.state.playerActionPermissions.single().id))
            result.error!! shouldContain "Cannot pay"
            result.state shouldBe before
            result.events shouldBe emptyList()
        }
    }
}
