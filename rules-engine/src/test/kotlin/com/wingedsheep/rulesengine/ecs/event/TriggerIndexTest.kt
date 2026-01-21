package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Tests for TriggerIndex performance optimization.
 */
class TriggerIndexTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    val soulWardenDef = CardDefinition.creature(
        name = "Soul Warden",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.CLERIC),
        power = 1,
        toughness = 1
    )

    val goblinPiledriverDef = CardDefinition.creature(
        name = "Goblin Piledriver",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.WARRIOR),
        power = 1,
        toughness = 2
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    // Registry that tracks which cards have which triggers
    class TestAbilityRegistry : AbilityRegistry {
        private val abilitiesByCard = mutableMapOf<String, List<TriggeredAbility>>()

        fun register(cardName: String, abilities: List<TriggeredAbility>) {
            abilitiesByCard[cardName] = abilities
        }

        override fun getTriggeredAbilities(
            entityId: EntityId,
            definition: CardDefinition
        ): List<TriggeredAbility> {
            return abilitiesByCard[definition.name] ?: emptyList()
        }
    }

    context("Basic indexing") {
        test("registers entity with OnEnterBattlefield trigger") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Soul Warden",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnOtherCreatureEnters(youControlOnly = false),
                        effect = GainLifeEffect(1)
                    )
                )
            )

            val index = TriggerIndex(registry)
            val creatureId = EntityId.generate()

            index.registerEntity(creatureId, soulWardenDef)

            index.getEntitiesForCategory(EventCategory.ENTER_BATTLEFIELD) shouldContain creatureId
            index.entityCount shouldBe 1
            index.registrations shouldBe 1
        }

        test("registers entity with multiple trigger types") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Goblin Piledriver",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnAttack(selfOnly = true),
                        effect = ModifyStatsEffect(2, 0, EffectTarget.Self)
                    ),
                    TriggeredAbility.create(
                        trigger = OnDeath(selfOnly = true),
                        effect = DealDamageEffect(1, EffectTarget.AnyTarget)
                    )
                )
            )

            val index = TriggerIndex(registry)
            val creatureId = EntityId.generate()

            index.registerEntity(creatureId, goblinPiledriverDef)

            index.getEntitiesForCategory(EventCategory.ATTACKER_DECLARED) shouldContain creatureId
            index.getEntitiesForCategory(EventCategory.CREATURE_DIED) shouldContain creatureId
            index.entityCount shouldBe 1
            index.size shouldBe 2 // Two category mappings
        }

        test("does not register entity without triggers") {
            val registry = TestAbilityRegistry()
            // No triggers registered for bears

            val index = TriggerIndex(registry)
            val creatureId = EntityId.generate()

            index.registerEntity(creatureId, bearDef)

            index.entityCount shouldBe 0
            index.registrations shouldBe 0
        }

        test("unregisters entity correctly") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Soul Warden",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnOtherCreatureEnters(youControlOnly = false),
                        effect = GainLifeEffect(1)
                    )
                )
            )

            val index = TriggerIndex(registry)
            val creatureId = EntityId.generate()

            index.registerEntity(creatureId, soulWardenDef)
            index.getEntitiesForCategory(EventCategory.ENTER_BATTLEFIELD) shouldContain creatureId

            index.unregisterEntity(creatureId)

            index.getEntitiesForCategory(EventCategory.ENTER_BATTLEFIELD).shouldBeEmpty()
            index.entityCount shouldBe 0
            index.unregistrations shouldBe 1
        }
    }

    context("Event to category mapping") {
        test("maps GameEvents to correct categories") {
            TriggerIndex.eventToCategory(
                GameEvent.EnteredBattlefield(EntityId.generate(), "Test", player1Id, null)
            ) shouldBe EventCategory.ENTER_BATTLEFIELD

            TriggerIndex.eventToCategory(
                GameEvent.CreatureDied(EntityId.generate(), "Test", player1Id)
            ) shouldBe EventCategory.CREATURE_DIED

            TriggerIndex.eventToCategory(
                GameEvent.AttackerDeclared(EntityId.generate(), "Test", player1Id, player2Id)
            ) shouldBe EventCategory.ATTACKER_DECLARED

            TriggerIndex.eventToCategory(
                GameEvent.UpkeepBegan(player1Id)
            ) shouldBe EventCategory.UPKEEP

            TriggerIndex.eventToCategory(
                GameEvent.SpellCast(EntityId.generate(), "Test", player1Id, false, true)
            ) shouldBe EventCategory.SPELL_CAST
        }

        test("maps Triggers to correct categories") {
            TriggerIndex.triggerToCategory(OnEnterBattlefield()) shouldBe EventCategory.ENTER_BATTLEFIELD
            TriggerIndex.triggerToCategory(OnOtherCreatureEnters()) shouldBe EventCategory.ENTER_BATTLEFIELD
            TriggerIndex.triggerToCategory(OnDeath()) shouldBe EventCategory.CREATURE_DIED
            TriggerIndex.triggerToCategory(OnAttack()) shouldBe EventCategory.ATTACKER_DECLARED
            TriggerIndex.triggerToCategory(OnUpkeep()) shouldBe EventCategory.UPKEEP
            TriggerIndex.triggerToCategory(OnSpellCast()) shouldBe EventCategory.SPELL_CAST
        }

        test("returns null for unmapped events") {
            TriggerIndex.eventToCategory(
                GameEvent.CardDiscarded(player1Id, EntityId.generate(), "Test")
            ) shouldBe null

            TriggerIndex.eventToCategory(
                GameEvent.LifeGained(player1Id, 3, 23)
            ) shouldBe null
        }
    }

    context("Index with game state") {
        test("rebuilds index from game state") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Soul Warden",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnOtherCreatureEnters(youControlOnly = false),
                        effect = GainLifeEffect(1)
                    )
                )
            )

            var state = newGame()

            // Create creatures on battlefield
            val warden1Id = EntityId.generate()
            val warden2Id = EntityId.generate()
            val bearId = EntityId.generate()

            val (_, state1) = state.createEntity(
                warden1Id,
                CardComponent(soulWardenDef, player1Id),
                ControllerComponent(player1Id)
            )
            val (_, state2) = state1.createEntity(
                warden2Id,
                CardComponent(soulWardenDef, player2Id),
                ControllerComponent(player2Id)
            )
            val (_, state3) = state2.createEntity(
                bearId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state3
                .addToZone(warden1Id, ZoneId.BATTLEFIELD)
                .addToZone(warden2Id, ZoneId.BATTLEFIELD)
                .addToZone(bearId, ZoneId.BATTLEFIELD)

            val index = TriggerIndex.forState(state, registry)

            // Both wardens should be indexed, bear should not
            val enterEntities = index.getEntitiesForCategory(EventCategory.ENTER_BATTLEFIELD)
            enterEntities shouldContainExactlyInAnyOrder listOf(warden1Id, warden2Id)
            index.entityCount shouldBe 2
        }

        test("getEntitiesForEvent returns correct entities") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Soul Warden",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnOtherCreatureEnters(youControlOnly = false),
                        effect = GainLifeEffect(1)
                    )
                )
            )
            registry.register(
                "Goblin Piledriver",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnAttack(selfOnly = true),
                        effect = ModifyStatsEffect(2, 0, EffectTarget.Self)
                    )
                )
            )

            val index = TriggerIndex(registry)
            val wardenId = EntityId.generate()
            val piledriverId = EntityId.generate()

            index.registerEntity(wardenId, soulWardenDef)
            index.registerEntity(piledriverId, goblinPiledriverDef)

            val enterEvent = GameEvent.EnteredBattlefield(EntityId.generate(), "Test", player1Id, null)
            val attackEvent = GameEvent.AttackerDeclared(piledriverId, "Goblin Piledriver", player1Id, player2Id)

            index.getEntitiesForEvent(enterEvent) shouldContain wardenId
            index.getEntitiesForEvent(attackEvent) shouldContain piledriverId
        }
    }

    context("TriggerDetector integration") {
        test("detector uses index for O(1) lookup") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Soul Warden",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnOtherCreatureEnters(youControlOnly = false),
                        effect = GainLifeEffect(1)
                    )
                )
            )

            var state = newGame()

            // Create Soul Warden on battlefield
            val wardenId = EntityId.generate()
            val (_, stateWithWarden) = state.createEntity(
                wardenId,
                CardComponent(soulWardenDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithWarden.addToZone(wardenId, ZoneId.BATTLEFIELD)

            // Create index
            val index = TriggerIndex.forState(state, registry)

            // Detect triggers for a creature entering
            val detector = TriggerDetector()
            val event = GameEvent.EnteredBattlefield(
                EntityId.generate(),
                "Some Creature",
                player2Id,
                null
            )

            val triggers = detector.detectTriggers(state, listOf(event), registry, index)

            triggers.size shouldBe 1
            triggers[0].sourceId shouldBe wardenId
            triggers[0].sourceName shouldBe "Soul Warden"
        }

        test("detector finds triggers without index (fallback)") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Soul Warden",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnOtherCreatureEnters(youControlOnly = false),
                        effect = GainLifeEffect(1)
                    )
                )
            )

            var state = newGame()

            val wardenId = EntityId.generate()
            val (_, stateWithWarden) = state.createEntity(
                wardenId,
                CardComponent(soulWardenDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithWarden.addToZone(wardenId, ZoneId.BATTLEFIELD)

            // Detect triggers WITHOUT index (should still work via fallback)
            val detector = TriggerDetector()
            val event = GameEvent.EnteredBattlefield(
                EntityId.generate(),
                "Some Creature",
                player2Id,
                null
            )

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers.size shouldBe 1
            triggers[0].sourceId shouldBe wardenId
        }
    }

    context("Statistics") {
        test("tracks lookup count") {
            val registry = TestAbilityRegistry()
            val index = TriggerIndex(registry)

            index.getEntitiesForCategory(EventCategory.ENTER_BATTLEFIELD)
            index.getEntitiesForCategory(EventCategory.CREATURE_DIED)
            index.getEntitiesForCategory(EventCategory.ATTACKER_DECLARED)

            index.lookups shouldBe 3
        }

        test("resetStats clears counters") {
            val registry = TestAbilityRegistry()
            registry.register(
                "Soul Warden",
                listOf(
                    TriggeredAbility.create(
                        trigger = OnOtherCreatureEnters(youControlOnly = false),
                        effect = GainLifeEffect(1)
                    )
                )
            )

            val index = TriggerIndex(registry)
            val id = EntityId.generate()

            index.registerEntity(id, soulWardenDef)
            index.getEntitiesForCategory(EventCategory.ENTER_BATTLEFIELD)
            index.unregisterEntity(id)

            index.resetStats()

            index.registrations shouldBe 0
            index.lookups shouldBe 0
            index.unregistrations shouldBe 0
        }
    }
})
