package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.combat.PreventNextDamageLeavingAmountExecutor
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageLeavingAmountEffect
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.serialization.json.Json

class PreventNextDamageLeavingAmountTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun install(game: TestGame, leave: Int = 1, combatOnly: Boolean = false,
                    target: EffectTarget = EffectTarget.Controller,
                    filter: GameObjectFilter = GameObjectFilter.Creature,
                    duration: Duration = Duration.EndOfTurn) {
            val effect = PreventNextDamageLeavingAmountEffect(DynamicAmount.Fixed(leave), target, filter,
                if (combatOnly) PreventionScope.CombatOnly else PreventionScope.AllDamage, duration)
            val result = PreventNextDamageLeavingAmountExecutor(services.dynamicAmountEvaluator).execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = game.player1Id,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))))
            result.error shouldBe null
            game.state = result.state
            val selected = game.selectCards(listOf(game.findPermanent("Hill Giant")!!))
            selected.error shouldBe null
            selected.events.filterIsInstance<DamagePreventionShieldCreatedEvent>().size shouldBe 1
        }
        fun damage(game: TestGame, amount: Int, combat: Boolean = false,
                   source: com.wingedsheep.sdk.model.EntityId = game.findPermanent("Hill Giant")!!,
                   target: com.wingedsheep.sdk.model.EntityId = game.player1Id): Int {
            val result = DamageUtils.applyDamagePreventionShields(game.state, target, amount, combat, source,
                predicateEvaluator = services.predicateEvaluator)
            game.state = result.state
            return result.remainingDamage
        }
        fun shieldCount(game: TestGame) = game.state.floatingEffects.count {
            it.effect.modification is SerializableModification.PreventNextDamageLeavingAmount
        }
        test("reduces one positive instance to the retained amount and consumes the shield") {
            val game = board(); install(game, leave = 2)
            damage(game, 7) shouldBe 2
            shieldCount(game) shouldBe 0
            damage(game, 7) shouldBe 7
        }
        test("a hit no larger than the remainder preserves the shield for later damage") {
            val game = board(); install(game, leave = 4)
            damage(game, 1) shouldBe 1
            shieldCount(game) shouldBe 1
            damage(game, 4) shouldBe 4
            shieldCount(game) shouldBe 1
            damage(game, 7) shouldBe 4
            shieldCount(game) shouldBe 0
        }
        test("zero damage does not spend the shield") {
            val game = board(); install(game)
            damage(game, 0) shouldBe 0
            shieldCount(game) shouldBe 1
            damage(game, 5) shouldBe 1
        }
        test("negative retained amount is clamped to zero") {
            val game = board(); install(game, leave = -2)
            damage(game, 5) shouldBe 0
        }
        test("noncombat damage leaves a combat-only shield unused") {
            val game = board(); install(game, combatOnly = true)
            damage(game, 5) shouldBe 5
            shieldCount(game) shouldBe 1
            damage(game, 5, combat = true) shouldBe 1
        }
        test("other sources and recipients leave the shield unused") {
            val game = board(); install(game)
            val bear = game.findPermanent("Grizzly Bears")!!
            damage(game, 5, source = bear) shouldBe 5
            damage(game, 5, target = bear) shouldBe 5
            shieldCount(game) shouldBe 1
            damage(game, 5) shouldBe 1
        }
        test("protects a permanent independently of its controller") {
            val game = board(); install(game, target = EffectTarget.ContextTarget(0))
            damage(game, 5) shouldBe 5
            damage(game, 5, target = game.findPermanent("Grizzly Bears")!!) shouldBe 1
        }
        test("unpreventable damage is dealt in full without reducing the shield") {
            val game = board(); install(game)
            game.state = game.state.copy(damageCantBePreventedThisTurn = true)
            damage(game, 5) shouldBe 5
            shieldCount(game) shouldBe 1
            game.state = game.state.copy(damageCantBePreventedThisTurn = false)
            damage(game, 5) shouldBe 1
        }
        test("required source properties are rechecked against projected state") {
            val game = board(); install(game, filter = GameObjectFilter.Creature.withColor(Color.RED))
            val giant = game.findPermanent("Hill Giant")!!
            val removed = game.state.getEntity(giant)!!
            game.state = game.state.updateEntity(giant) { it.with(FaceDownComponent) }
            // The printed red card is colorless face down, so it no longer qualifies.
            damage(game, 5) shouldBe 5
            shieldCount(game) shouldBe 1
            game.state = game.state.withEntity(giant, removed)
            damage(game, 5) shouldBe 1
        }
        test("chosen source cannot carry the shield through a new zone visit") {
            val game = board(); install(game)
            val giant = game.findPermanent("Hill Giant")!!
            val battlefield = game.state.zones.keys.single { it.zoneType == Zone.BATTLEFIELD && giant in game.state.getZone(it) }
            game.state = game.state.removeFromZone(battlefield, giant)
                .addToZone(ZoneKey(game.player2Id, Zone.EXILE), giant)
                .removeFromZone(ZoneKey(game.player2Id, Zone.EXILE), giant).addToZone(battlefield, giant)
            damage(game, 5) shouldBe 5
        }
        test("shield and pending source choice serialize with their full context") {
            val game = board()
            val result = PreventNextDamageLeavingAmountExecutor(services.dynamicAmountEvaluator).execute(game.state,
                PreventNextDamageLeavingAmountEffect(DynamicAmount.XValue),
                EffectContext(sourceId = null, controllerId = game.player1Id, xValue = 2))
            result.pendingDecision.shouldNotBeNull()
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            val paused = json.decodeFromString<GameState>(json.encodeToString<GameState>(result.state))
            game.state = paused
            game.selectCards(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.state = json.decodeFromString<GameState>(json.encodeToString<GameState>(game.state))
            val event: GameEvent = DamagePreventionShieldCreatedEvent(game.player1Id, game.state.floatingEffects.last().id)
            json.decodeFromString<GameEvent>(json.encodeToString<GameEvent>(event)) shouldBe event
            damage(game, 5) shouldBe 2
        }
        test("end-of-combat duration expires without consuming damage") {
            val game = board(); install(game, duration = Duration.EndOfCombat)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            shieldCount(game) shouldBe 0
        }
    }
}
