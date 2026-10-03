package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.event.GrantedReplacementEffect
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageUnpreventableThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PerPointCounterPreventionTest : ScenarioTestBase() {
    init {
        val body = card("Counter Prevention Test Body") {
            typeLine = "Creature — Human"; power = 2; toughness = 8
            replacementEffect(PreventDamagePerCounter(CounterType.CHARGE))
        }
        cardRegistry.register(body)
        fun board(count: Int = 3) = scenario().withPlayers("One", "Two")
            .withCardOnBattlefield(1, body.name).withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build().also { game ->
                val id = game.findPermanent(body.name)!!
                game.state = game.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.CHARGE to count))) }
            }
        fun apply(game: TestGame, damage: Int, combat: Boolean = false) = DamageUtils.applyPerPointCounterPrevention(
            game.state, game.findPermanent(body.name)!!, damage, game.findPermanent("Grizzly Bears"), combat, services.predicateEvaluator
        )
        fun count(state: GameState, game: TestGame) = state.getEntity(game.findPermanent(body.name)!!)!!
            .get<CountersComponent>()!!.getCount(CounterType.CHARGE)
        for ((damage, remaining, counters) in listOf(Triple(2, 0, 1), Triple(3, 0, 0), Triple(5, 2, 0))) {
            test("$damage damage removes only available counters and leaves $remaining damage") {
                val game = board(); val before = game.state
                val result = apply(game, damage)
                result.remainingDamage shouldBe remaining
                count(result.state, game) shouldBe counters
                count(before, game) shouldBe 3
                val event = result.events.single() as CountersRemovedEvent
                event.amount shouldBe minOf(3, damage)
                event.remainingCount shouldBe counters
                event.byDamagePrevention shouldBe true
            }
        }
        test("zero damage and zero counters emit no changes") {
            for ((counters, damage) in listOf(3 to 0, 0 to 4)) {
                val game = board(counters); val result = apply(game, damage)
                result.state shouldBe game.state
                result.events shouldBe emptyList()
                result.remainingDamage shouldBe damage
            }
        }
        test("unpreventable damage removes counters and deals the entire amount") {
            val game = board(); val id = game.findPermanent(body.name)!!
            game.state = game.state.updateEntity(id) { it.with(DamageUnpreventableThisTurnComponent) }
            val result = apply(game, 5)
            result.remainingDamage shouldBe 5
            count(result.state, game) shouldBe 0
            result.events.size shouldBe 1
        }
        test("an explicitly unpreventable damage effect spends counters and deals full damage") {
            val game = board()
            val id = game.findPermanent(body.name)!!
            val result = services.effectExecutorRegistry.execute(
                game.state, Effects.DealDamage(4, EffectTarget.Self, cantBePrevented = true),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = id, controllerId = game.player1Id)
            )
            result.outcome shouldBe com.wingedsheep.engine.core.Outcome.Done
            count(result.newState, game) shouldBe 0
            result.newState.getEntity(id)!!.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()!!.amount shouldBe 4
            result.events.filterIsInstance<CountersRemovedEvent>().single().amount shouldBe 3
            result.events.filterIsInstance<com.wingedsheep.engine.core.DamageDealtEvent>().single().amount shouldBe 4
        }
        test("combat type amount and source filters all restrict applicability") {
            val game = board(); val id = game.findPermanent(body.name)!!
            fun replace(pattern: EventPattern.DamageEvent) {
                game.state = game.state.updateEntity(id) { it.with(ReplacementEffectSourceComponent(
                    listOf(PreventDamagePerCounter(CounterType.CHARGE, pattern)))) }
            }
            replace(EventPattern.DamageEvent(recipient = Recipient.Self, damageType = DamageType.Combat))
            apply(game, 2).remainingDamage shouldBe 2
            apply(game, 2, combat = true).remainingDamage shouldBe 0
            replace(EventPattern.DamageEvent(recipient = Recipient.Self, amount = AmountFilter.Exactly(3)))
            apply(game, 2).remainingDamage shouldBe 2
            apply(game, 3).remainingDamage shouldBe 0
            replace(EventPattern.DamageEvent(recipient = Recipient.Self, source = GameObjectFilter.Creature.withColor(Color.RED)))
            apply(game, 2).remainingDamage shouldBe 2
            replace(EventPattern.DamageEvent(recipient = Recipient.Self, source = GameObjectFilter.Creature.withColor(Color.GREEN)))
            apply(game, 2).remainingDamage shouldBe 0
        }
        test("source qualification reads projected color after a continuous color change") {
            val game = board(); val id = game.findPermanent(body.name)!!; val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(id) { it.with(ReplacementEffectSourceComponent(listOf(
                PreventDamagePerCounter(CounterType.CHARGE, EventPattern.DamageEvent(recipient = Recipient.Self,
                    source = GameObjectFilter.Creature.withColor(Color.RED)))))) }
            apply(game, 2).remainingDamage shouldBe 2
            val changed = services.effectExecutorRegistry.execute(game.state, Effects.ChangeColor(EffectTarget.Self, setOf(Color.RED)),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = bear, controllerId = game.player2Id))
            game.state = changed.newState
            apply(game, 2).remainingDamage shouldBe 0
        }
        test("a battlefield host can protect another matching permanent") {
            val game = board(); val id = game.findPermanent(body.name)!!; val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(id) { it.with(ReplacementEffectSourceComponent(listOf(
                PreventDamagePerCounter(CounterType.CHARGE, EventPattern.DamageEvent(recipient = Recipient.Any))))) }
                .updateEntity(bear) { it.with(CountersComponent(mapOf(CounterType.CHARGE to 1))) }
            val result = DamageUtils.applyPerPointCounterPrevention(game.state, bear, 3, id, false, services.predicateEvaluator)
            result.remainingDamage shouldBe 2
            result.state.getEntity(bear)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 0
            count(result.state, game) shouldBe 3
        }
        test("printed ability is suppressed while face down or abilities are lost") {
            val faceDown = board(); val id = faceDown.findPermanent(body.name)!!
            faceDown.state = faceDown.state.updateEntity(id) { it.with(FaceDownComponent) }
            apply(faceDown, 2).remainingDamage shouldBe 2
            val lost = board(); val lostId = lost.findPermanent(body.name)!!
            val result = services.effectExecutorRegistry.execute(lost.state, Effects.RemoveAllAbilities(EffectTarget.Self),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = lostId, controllerId = lost.player1Id))
            lost.state = result.newState
            lost.state.projectedState.hasLostAllAbilities(lostId) shouldBe true
            apply(lost, 2).remainingDamage shouldBe 2
        }
        test("a granted effect on a face-down permanent emits only its public label") {
            val game = board(); val id = game.findPermanent(body.name)!!
            game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
                .copy(grantedReplacementEffects = listOf(GrantedReplacementEffect(id, game.player1Id,
                    PreventDamagePerCounter(CounterType.CHARGE), Duration.EndOfTurn)))
            val result = apply(game, 2)
            result.remainingDamage shouldBe 0
            val event = result.events.single() as CountersRemovedEvent
            event.entityName shouldBe "Face-down permanent"
            count(result.state, game) shouldBe 1
            val client = com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(event), game.player2Id, result.state).single() as
                com.wingedsheep.engine.view.ClientEvent.CounterRemoved
            client.permanentName shouldBe "Face-down permanent"
        }
        test("multiple copies cannot spend counters twice for prevented damage") {
            val game = board(); val id = game.findPermanent(body.name)!!
            game.state = game.state.updateEntity(id) { it.with(ReplacementEffectSourceComponent(listOf(
                PreventDamagePerCounter(CounterType.CHARGE), PreventDamagePerCounter(CounterType.CHARGE)))) }
            val result = apply(game, 2)
            result.remainingDamage shouldBe 0
            count(result.state, game) shouldBe 1
            result.events.size shouldBe 1
        }
        test("durational grants serialize and expire through normal cleanup") {
            val game = board(); val id = game.findPermanent(body.name)!!
            game.state = game.state.updateEntity(id) { it.without<ReplacementEffectSourceComponent>() }
                .copy(grantedReplacementEffects = listOf(GrantedReplacementEffect(id, game.player1Id,
                    PreventDamagePerCounter(CounterType.CHARGE), Duration.EndOfTurn)))
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            game.state = json.decodeFromString<GameState>(json.encodeToString(game.state))
            apply(game, 2).remainingDamage shouldBe 0
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.grantedReplacementEffects shouldBe emptyList()
            apply(game, 2).remainingDamage shouldBe 2
        }
    }
}
