package com.wingedsheep.rulesengine.ecs.script

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EffectExecutorTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addBear(controller: EntityId = player1Id): Pair<EntityId, GameState> {
        val (bearId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(bearDef, controller),
            ControllerComponent(controller)
        )
        return bearId to state1.addToZone(bearId, ZoneId.BATTLEFIELD)
    }

    val executor = EffectExecutor()

    context("life gain effects") {
        test("controller gains life") {
            val state = newGame()
            val effect = GainLifeEffect(3, EffectTarget.Controller)
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            val lifeComponent = result.state.getEntity(player1Id)?.get<LifeComponent>()
            lifeComponent?.life shouldBe 23  // 20 starting + 3

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<EffectEvent.LifeGained>()
        }

        test("opponent gains life") {
            val state = newGame()
            val effect = GainLifeEffect(5, EffectTarget.Opponent)
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            val lifeComponent = result.state.getEntity(player2Id)?.get<LifeComponent>()
            lifeComponent?.life shouldBe 25  // 20 starting + 5
        }
    }

    context("life loss effects") {
        test("opponent loses life") {
            val state = newGame()
            val effect = LoseLifeEffect(4, EffectTarget.Opponent)
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            val lifeComponent = result.state.getEntity(player2Id)?.get<LifeComponent>()
            lifeComponent?.life shouldBe 16  // 20 starting - 4

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<EffectEvent.LifeLost>()
        }
    }

    context("damage effects") {
        test("deal damage to player") {
            val state = newGame()
            val effect = DealDamageEffect(3, EffectTarget.Opponent)
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            val lifeComponent = result.state.getEntity(player2Id)?.get<LifeComponent>()
            lifeComponent?.life shouldBe 17  // 20 - 3

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<EffectEvent.DamageDealtToPlayer>()
        }

        test("deal damage to creature") {
            val (bearId, state) = newGame().addBear()
            val effect = DealDamageEffect(1, EffectTarget.TargetCreature)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(bearId))
            )

            val result = executor.execute(state, effect, context)

            val damageComponent = result.state.getEntity(bearId)?.get<DamageComponent>()
            damageComponent?.amount shouldBe 1

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<EffectEvent.DamageDealtToCreature>()
        }
    }

    context("tap/untap effects") {
        test("tap a creature") {
            val (bearId, state) = newGame().addBear()
            val effect = TapUntapEffect(EffectTarget.TargetCreature, tap = true)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(bearId))
            )

            val result = executor.execute(state, effect, context)

            result.state.getEntity(bearId)?.has<TappedComponent>() shouldBe true
            result.events.first().shouldBeInstanceOf<EffectEvent.PermanentTapped>()
        }

        test("untap a creature") {
            val (bearId, state1) = newGame().addBear()
            // First tap the creature
            val state = state1.updateEntity(bearId) { it.with(TappedComponent) }

            val effect = TapUntapEffect(EffectTarget.TargetCreature, tap = false)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(bearId))
            )

            val result = executor.execute(state, effect, context)

            result.state.getEntity(bearId)?.has<TappedComponent>() shouldBe false
            result.events.first().shouldBeInstanceOf<EffectEvent.PermanentUntapped>()
        }
    }

    context("destroy effects") {
        test("destroy a creature moves it to graveyard") {
            val (bearId, state) = newGame().addBear()
            val effect = DestroyEffect(EffectTarget.TargetCreature)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(bearId))
            )

            val result = executor.execute(state, effect, context)

            // Bear should be in graveyard, not battlefield
            result.state.getZone(ZoneId.BATTLEFIELD).contains(bearId) shouldBe false
            result.state.getZone(ZoneId(ZoneType.GRAVEYARD, player1Id)).contains(bearId) shouldBe true

            result.events shouldHaveSize 2  // Destroyed + CreatureDied
            result.events.any { it is EffectEvent.PermanentDestroyed } shouldBe true
            result.events.any { it is EffectEvent.CreatureDied } shouldBe true
        }
    }

    context("counter effects") {
        test("add +1/+1 counters to creature") {
            val (bearId, state) = newGame().addBear()
            val effect = AddCountersEffect("+1/+1", 2, EffectTarget.TargetCreature)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(bearId))
            )

            val result = executor.execute(state, effect, context)

            val counters = result.state.getEntity(bearId)?.get<CountersComponent>()
            counters?.plusOnePlusOneCount shouldBe 2

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<EffectEvent.CountersAdded>()
        }

        test("add counters to self") {
            val (bearId, state) = newGame().addBear()
            val effect = AddCountersEffect("+1/+1", 1, EffectTarget.Self)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = bearId,  // Source is the bear itself
                targets = emptyList()
            )

            val result = executor.execute(state, effect, context)

            val counters = result.state.getEntity(bearId)?.get<CountersComponent>()
            counters?.plusOnePlusOneCount shouldBe 1
        }
    }

    context("mana effects") {
        test("add colored mana") {
            val state = newGame()
            val effect = AddManaEffect(Color.GREEN, 1)
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            val manaPool = result.state.getEntity(player1Id)?.get<ManaPoolComponent>()
            manaPool?.pool?.green shouldBe 1

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<EffectEvent.ManaAdded>()
        }

        test("add colorless mana") {
            val state = newGame()
            val effect = AddColorlessManaEffect(3)
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            val manaPool = result.state.getEntity(player1Id)?.get<ManaPoolComponent>()
            manaPool?.pool?.colorless shouldBe 3
        }
    }

    context("composite effects") {
        test("executes all effects in sequence") {
            val state = newGame()
            val effect = CompositeEffect(
                listOf(
                    GainLifeEffect(2, EffectTarget.Controller),
                    LoseLifeEffect(3, EffectTarget.Opponent)
                )
            )
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            val player1Life = result.state.getEntity(player1Id)?.get<LifeComponent>()?.life
            val player2Life = result.state.getEntity(player2Id)?.get<LifeComponent>()?.life

            player1Life shouldBe 22  // 20 + 2
            player2Life shouldBe 17  // 20 - 3

            result.events shouldHaveSize 2
        }
    }

    context("destroy all effects") {
        test("destroy all creatures") {
            val (bear1Id, state1) = newGame().addBear(player1Id)
            val (bear2Id, state2) = state1.addBear(player2Id)

            val effect = DestroyAllCreaturesEffect
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state2, effect, context)

            // Both creatures should be in graveyards
            result.state.getZone(ZoneId.BATTLEFIELD) shouldHaveSize 0
            result.state.getZone(ZoneId(ZoneType.GRAVEYARD, player1Id)).contains(bear1Id) shouldBe true
            result.state.getZone(ZoneId(ZoneType.GRAVEYARD, player2Id)).contains(bear2Id) shouldBe true
        }

        test("destroy all lands") {
            var state = newGame()

            // Add forests for both players
            val (forest1Id, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(forest1Id, ZoneId.BATTLEFIELD)

            val (forest2Id, state3) = state.createEntity(
                EntityId.generate(),
                CardComponent(forestDef, player2Id),
                ControllerComponent(player2Id)
            )
            state = state3.addToZone(forest2Id, ZoneId.BATTLEFIELD)

            // Add a creature (should not be destroyed)
            val (bearId, state4) = state.addBear()
            state = state4

            val effect = DestroyAllLandsEffect
            val context = ExecutionContext(player1Id, player1Id)

            val result = executor.execute(state, effect, context)

            // Lands should be in graveyards
            result.state.getZone(ZoneId(ZoneType.GRAVEYARD, player1Id)).contains(forest1Id) shouldBe true
            result.state.getZone(ZoneId(ZoneType.GRAVEYARD, player2Id)).contains(forest2Id) shouldBe true

            // Creature should still be on battlefield
            result.state.getZone(ZoneId.BATTLEFIELD).contains(bearId) shouldBe true
        }
    }

    context("temporary modifier effects") {
        test("grant keyword until end of turn creates temporary modifier") {
            val (bearId, state) = newGame().addBear()
            val effect = GrantKeywordUntilEndOfTurnEffect(Keyword.FLYING, EffectTarget.TargetCreature)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(bearId))
            )

            val result = executor.execute(state, effect, context)

            result.temporaryModifiers shouldHaveSize 1
            val modifier = result.temporaryModifiers.first()
            modifier.modification.shouldBeInstanceOf<com.wingedsheep.rulesengine.ecs.layers.Modification.AddKeyword>()
        }

        test("modify stats until end of turn creates temporary modifier") {
            val (bearId, state) = newGame().addBear()
            val effect = ModifyStatsEffect(3, 3, EffectTarget.TargetCreature, untilEndOfTurn = true)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(bearId))
            )

            val result = executor.execute(state, effect, context)

            result.temporaryModifiers shouldHaveSize 1
        }
    }
})
