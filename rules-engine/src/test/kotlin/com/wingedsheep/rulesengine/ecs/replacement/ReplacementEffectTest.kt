package com.wingedsheep.rulesengine.ecs.replacement

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ReplacementEffectTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    context("DamageReplacementEffect") {
        test("prevents all damage with PreventAll modifier") {
            val state = newGame()
            val sourceId = EntityId.generate()

            // Create a "Prevent all damage" effect (like Fog)
            val fogEffect = DamageReplacementEffect(
                sourceId = EntityId.of("fog"),
                description = "Prevent all damage",
                controllerId = player1Id,
                filter = DamageFilter.AllDamage,
                modifyAmount = DamageModifier.PreventAll
            )

            // Damage event to player
            val damageEvent = GameActionEvent.DamageDealtToPlayer(
                sourceId = sourceId,
                targetId = player1Id,
                amount = 5
            )

            // Effect should apply
            fogEffect.appliesTo(damageEvent, state) shouldBe true

            // Effect should prevent damage
            val result = fogEffect.apply(damageEvent, state)
            result.shouldBeInstanceOf<ReplacementResult.Replaced>()
            (result as ReplacementResult.Replaced).newEvents.shouldBeEmpty()
        }

        test("doubles damage with Double modifier") {
            val state = newGame()

            // Create a "Double all damage" effect (like Dictate of the Twin Gods)
            val doubleEffect = DamageReplacementEffect(
                sourceId = EntityId.of("dictate"),
                description = "Double all damage",
                controllerId = player1Id,
                filter = DamageFilter.AllDamage,
                modifyAmount = DamageModifier.Double
            )

            val damageEvent = GameActionEvent.DamageDealtToPlayer(
                sourceId = EntityId.generate(),
                targetId = player2Id,
                amount = 3
            )

            val result = doubleEffect.apply(damageEvent, state)
            result.shouldBeInstanceOf<ReplacementResult.Modified>()
            val modified = (result as ReplacementResult.Modified).modifiedEvent as GameActionEvent.DamageDealtToPlayer
            modified.amount shouldBe 6
        }

        test("reduces damage with ReduceBy modifier") {
            val state = newGame()

            val reduceEffect = DamageReplacementEffect(
                sourceId = EntityId.of("shield"),
                description = "Reduce damage by 2",
                controllerId = player1Id,
                filter = DamageFilter.ToYou,
                modifyAmount = DamageModifier.ReduceBy(2)
            )

            val damageEvent = GameActionEvent.DamageDealtToPlayer(
                sourceId = EntityId.generate(),
                targetId = player1Id,
                amount = 5
            )

            val result = reduceEffect.apply(damageEvent, state)
            result.shouldBeInstanceOf<ReplacementResult.Modified>()
            val modified = (result as ReplacementResult.Modified).modifiedEvent as GameActionEvent.DamageDealtToPlayer
            modified.amount shouldBe 3
        }

        test("ToYou filter only matches damage to controller") {
            val state = newGame()

            val effect = DamageReplacementEffect(
                sourceId = EntityId.of("effect"),
                description = "Prevent damage to you",
                controllerId = player1Id,
                filter = DamageFilter.ToYou,
                modifyAmount = DamageModifier.PreventAll
            )

            // Damage to player1 (controller) should match
            val damageToPlayer1 = GameActionEvent.DamageDealtToPlayer(
                sourceId = EntityId.generate(),
                targetId = player1Id,
                amount = 3
            )
            effect.appliesTo(damageToPlayer1, state) shouldBe true

            // Damage to player2 (opponent) should NOT match
            val damageToPlayer2 = GameActionEvent.DamageDealtToPlayer(
                sourceId = EntityId.generate(),
                targetId = player2Id,
                amount = 3
            )
            effect.appliesTo(damageToPlayer2, state) shouldBe false
        }

        test("ToCreaturesYouControl filter matches damage to creatures you control") {
            var state = newGame()

            // Create creature controlled by player1
            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val effect = DamageReplacementEffect(
                sourceId = EntityId.of("effect"),
                description = "Prevent damage to creatures you control",
                controllerId = player1Id,
                filter = DamageFilter.ToCreaturesYouControl,
                modifyAmount = DamageModifier.PreventAll
            )

            // Damage to player1's creature should match
            val damageToCreature = GameActionEvent.DamageDealtToCreature(
                sourceId = EntityId.generate(),
                targetId = creatureId,
                amount = 2
            )
            effect.appliesTo(damageToCreature, state) shouldBe true
        }
    }

    context("DrawReplacementEffect") {
        test("skips opponent's draw") {
            val state = newGame()

            // Create a "Skip opponent's draws" effect
            val skipEffect = DrawReplacementEffect(
                sourceId = EntityId.of("spirit_of_labyrinth"),
                description = "Opponents can't draw cards",
                controllerId = player1Id,
                affectsPlayers = DrawPlayerFilter.Opponents,
                replacement = DrawReplacement.Skip
            )

            // Opponent's draw should be affected
            val opponentDraw = GameActionEvent.CardDrawn(
                playerId = player2Id,
                cardId = EntityId.generate(),
                cardName = "Test Card"
            )
            skipEffect.appliesTo(opponentDraw, state) shouldBe true

            val result = skipEffect.apply(opponentDraw, state)
            result.shouldBeInstanceOf<ReplacementResult.Replaced>()
            (result as ReplacementResult.Replaced).newEvents.shouldBeEmpty()
        }

        test("does not affect controller's own draw") {
            val state = newGame()

            val skipEffect = DrawReplacementEffect(
                sourceId = EntityId.of("spirit_of_labyrinth"),
                description = "Opponents can't draw cards",
                controllerId = player1Id,
                affectsPlayers = DrawPlayerFilter.Opponents,
                replacement = DrawReplacement.Skip
            )

            // Controller's own draw should NOT be affected
            val ownDraw = GameActionEvent.CardDrawn(
                playerId = player1Id,
                cardId = EntityId.generate(),
                cardName = "Test Card"
            )
            skipEffect.appliesTo(ownDraw, state) shouldBe false
        }
    }

    context("CounterReplacementEffect") {
        test("adds extra +1/+1 counter (Hardened Scales)") {
            var state = newGame()

            // Create creature controlled by player1
            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Hardened Scales: "If one or more +1/+1 counters would be put on a creature you control,
            // that many plus one are put on instead"
            val hardenedScalesEffect = CounterReplacementEffect(
                sourceId = EntityId.of("hardened_scales"),
                description = "Add one extra +1/+1 counter",
                controllerId = player1Id,
                filter = CounterFilter.PlusOnePlusOneOnYourCreatures,
                modifier = CounterModifier.AddN(1)
            )

            val counterEvent = GameActionEvent.CounterAdded(
                entityId = creatureId,
                name = "Grizzly Bears",
                counterType = "PLUS_ONE_PLUS_ONE",
                count = 2
            )

            hardenedScalesEffect.appliesTo(counterEvent, state) shouldBe true

            val result = hardenedScalesEffect.apply(counterEvent, state)
            result.shouldBeInstanceOf<ReplacementResult.Modified>()
            val modified = (result as ReplacementResult.Modified).modifiedEvent as GameActionEvent.CounterAdded
            modified.count shouldBe 3 // 2 + 1
        }

        test("doubles counters (Doubling Season)") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val doublingSeason = CounterReplacementEffect(
                sourceId = EntityId.of("doubling_season"),
                description = "Double counters on permanents you control",
                controllerId = player1Id,
                filter = CounterFilter.AllCountersOnYourPermanents,
                modifier = CounterModifier.Double
            )

            val counterEvent = GameActionEvent.CounterAdded(
                entityId = creatureId,
                name = "Grizzly Bears",
                counterType = "PLUS_ONE_PLUS_ONE",
                count = 3
            )

            val result = doublingSeason.apply(counterEvent, state)
            result.shouldBeInstanceOf<ReplacementResult.Modified>()
            val modified = (result as ReplacementResult.Modified).modifiedEvent as GameActionEvent.CounterAdded
            modified.count shouldBe 6 // 3 * 2
        }

        test("doesn't affect opponent's creatures") {
            var state = newGame()

            // Creature controlled by player2 (opponent)
            val opponentCreatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                opponentCreatureId,
                CardComponent(bearDef, player2Id),
                ControllerComponent(player2Id)
            )
            state = stateWithCreature.addToZone(opponentCreatureId, ZoneId.BATTLEFIELD)

            val hardenedScalesEffect = CounterReplacementEffect(
                sourceId = EntityId.of("hardened_scales"),
                description = "Add one extra +1/+1 counter",
                controllerId = player1Id,
                filter = CounterFilter.PlusOnePlusOneOnYourCreatures,
                modifier = CounterModifier.AddN(1)
            )

            val counterEvent = GameActionEvent.CounterAdded(
                entityId = opponentCreatureId,
                name = "Grizzly Bears",
                counterType = "PLUS_ONE_PLUS_ONE",
                count = 2
            )

            // Should NOT apply to opponent's creature
            hardenedScalesEffect.appliesTo(counterEvent, state) shouldBe false
        }
    }

    context("ReplacementEffectResolver") {
        val resolver = ReplacementEffectResolver()

        test("applies single replacement effect") {
            val state = newGame()

            val preventAllEffect = DamageReplacementEffect(
                sourceId = EntityId.of("fog"),
                description = "Prevent all damage",
                controllerId = player1Id,
                filter = DamageFilter.AllDamage,
                modifyAmount = DamageModifier.PreventAll
            )

            val damageEvent = GameActionEvent.DamageDealtToPlayer(
                sourceId = EntityId.generate(),
                targetId = player1Id,
                amount = 5
            )

            val result = resolver.applyReplacements(state, listOf(damageEvent), listOf(preventAllEffect))

            result.events.shouldBeEmpty() // Damage prevented
            result.appliedReplacements shouldHaveSize 1
            result.appliedReplacements[0].description shouldBe "Prevent all damage"
        }

        test("applies multiple replacement effects in sequence") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Hardened Scales: +1 counter
            val hardenedScales = CounterReplacementEffect(
                sourceId = EntityId.of("hardened_scales"),
                description = "Hardened Scales",
                controllerId = player1Id,
                filter = CounterFilter.PlusOnePlusOneOnYourCreatures,
                modifier = CounterModifier.AddN(1)
            )

            // Doubling Season: double counters
            val doublingSeason = CounterReplacementEffect(
                sourceId = EntityId.of("doubling_season"),
                description = "Doubling Season",
                controllerId = player1Id,
                filter = CounterFilter.AllCountersOnYourPermanents,
                modifier = CounterModifier.Double
            )

            val counterEvent = GameActionEvent.CounterAdded(
                entityId = creatureId,
                name = "Grizzly Bears",
                counterType = "PLUS_ONE_PLUS_ONE",
                count = 1
            )

            // Both effects should apply
            // Order: Hardened Scales (1 → 2), then Doubling Season (2 → 4)
            val result = resolver.applyReplacements(state, listOf(counterEvent), listOf(hardenedScales, doublingSeason))

            result.events shouldHaveSize 1
            val finalEvent = result.events[0] as GameActionEvent.CounterAdded
            finalEvent.count shouldBe 4 // 1 + 1 = 2, 2 * 2 = 4

            result.appliedReplacements shouldHaveSize 2
        }

        test("each replacement effect only applies once per event") {
            val state = newGame()

            // Effect that doubles damage
            val doubleEffect = DamageReplacementEffect(
                sourceId = EntityId.of("dictate"),
                description = "Double damage",
                controllerId = player1Id,
                filter = DamageFilter.AllDamage,
                modifyAmount = DamageModifier.Double
            )

            val damageEvent = GameActionEvent.DamageDealtToPlayer(
                sourceId = EntityId.generate(),
                targetId = player2Id,
                amount = 2
            )

            val result = resolver.applyReplacements(state, listOf(damageEvent), listOf(doubleEffect))

            // Damage should be doubled only once, not infinitely
            result.events shouldHaveSize 1
            val finalEvent = result.events[0] as GameActionEvent.DamageDealtToPlayer
            finalEvent.amount shouldBe 4 // 2 * 2 = 4 (not 8, 16, etc.)
        }

        test("passes through events without applicable replacements") {
            val state = newGame()

            // Effect only applies to player1
            val effect = DamageReplacementEffect(
                sourceId = EntityId.of("effect"),
                description = "Prevent damage to you",
                controllerId = player1Id,
                filter = DamageFilter.ToYou,
                modifyAmount = DamageModifier.PreventAll
            )

            // Damage to player2 - should NOT be affected
            val damageEvent = GameActionEvent.DamageDealtToPlayer(
                sourceId = EntityId.generate(),
                targetId = player2Id,
                amount = 3
            )

            val result = resolver.applyReplacements(state, listOf(damageEvent), listOf(effect))

            // Event should pass through unchanged
            result.events shouldHaveSize 1
            val finalEvent = result.events[0] as GameActionEvent.DamageDealtToPlayer
            finalEvent.amount shouldBe 3
            result.appliedReplacements.shouldBeEmpty()
        }
    }
})
