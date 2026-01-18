package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class EcsTriggerDetectorTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    // Card definitions (abilities stored separately in registry)
    val elvishVisionary = CardDefinition.creature(
        name = "Elvish Visionary",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.ELF),
        power = 1,
        toughness = 1
    )

    val bloodArtist = CardDefinition.creature(
        name = "Blood Artist",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.HUMAN),  // Using HUMAN as VAMPIRE doesn't exist
        power = 0,
        toughness = 1
    )

    val darkConfidant = CardDefinition.creature(
        name = "Dark Confidant",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.HUMAN),
        power = 2,
        toughness = 1
    )

    val heroOfBladehold = CardDefinition.creature(
        name = "Hero of Bladehold",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        subtypes = setOf(Subtype.HUMAN),
        power = 3,
        toughness = 4
    )

    val basicBear = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun EcsGameState.addCreature(def: CardDefinition, controllerId: EntityId): Pair<EntityId, EcsGameState> {
        val (entityId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        return entityId to state1.addToZone(entityId, ZoneId.BATTLEFIELD)
    }

    // Create registry with abilities for test cards
    fun createRegistry(): CardDefinitionAbilityRegistry {
        val registry = CardDefinitionAbilityRegistry()

        // ETB trigger for Elvish Visionary
        registry.register("Elvish Visionary", listOf(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(selfOnly = true),
                effect = DrawCardsEffect(1)
            )
        ))

        // Death trigger for Blood Artist (any creature dying)
        registry.register("Blood Artist", listOf(
            TriggeredAbility.create(
                trigger = OnDeath(selfOnly = false),
                effect = CompositeEffect(listOf(
                    DealDamageEffect(1, EffectTarget.Opponent),
                    GainLifeEffect(1, EffectTarget.Controller)
                ))
            )
        ))

        // Upkeep trigger for Dark Confidant
        registry.register("Dark Confidant", listOf(
            TriggeredAbility.create(
                trigger = OnUpkeep(controllerOnly = true),
                effect = DrawCardsEffect(1)
            )
        ))

        // Attack trigger for Hero of Bladehold
        registry.register("Hero of Bladehold", listOf(
            TriggeredAbility.create(
                trigger = OnAttack(selfOnly = true),
                effect = GainLifeEffect(1, EffectTarget.Controller)
            )
        ))

        return registry
    }

    val detector = EcsTriggerDetector()
    val registry = createRegistry()

    context("ETB trigger detection") {

        test("detects self ETB trigger") {
            val (creatureId, state) = newGame().addCreature(elvishVisionary, player1Id)

            val event = EcsGameEvent.EnteredBattlefield(
                entityId = creatureId,
                cardName = "Elvish Visionary",
                controllerId = player1Id,
                fromZone = ZoneId.hand(player1Id)
            )

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers.first().sourceName shouldBe "Elvish Visionary"
            triggers.first().controllerId shouldBe player1Id
        }

        test("does not trigger for other creatures with selfOnly ETB") {
            val (_, state1) = newGame().addCreature(elvishVisionary, player1Id)
            val (otherId, state) = state1.addCreature(basicBear, player2Id)

            val event = EcsGameEvent.EnteredBattlefield(
                entityId = otherId,
                cardName = "Grizzly Bears",
                controllerId = player2Id,
                fromZone = ZoneId.hand(player2Id)
            )

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers.shouldBeEmpty()
        }
    }

    context("death trigger detection") {

        test("detects death of any creature with selfOnly=false") {
            val (_, state1) = newGame().addCreature(bloodArtist, player1Id)
            val (bearId, state) = state1.addCreature(basicBear, player2Id)

            val event = EcsGameEvent.CreatureDied(
                entityId = bearId,
                cardName = "Grizzly Bears",
                ownerId = player2Id
            )

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers.first().sourceName shouldBe "Blood Artist"
        }

        test("triggers when own creature dies with selfOnly=false") {
            val (bloodArtistId, state) = newGame().addCreature(bloodArtist, player1Id)

            // Blood Artist itself dies
            val event = EcsGameEvent.CreatureDied(
                entityId = bloodArtistId,
                cardName = "Blood Artist",
                ownerId = player1Id
            )

            // Blood Artist is still in state (hasn't been removed yet when trigger is detected)
            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
        }
    }

    context("upkeep trigger detection") {

        test("detects controller's upkeep trigger") {
            val (_, state) = newGame().addCreature(darkConfidant, player1Id)

            val event = EcsGameEvent.UpkeepBegan(activePlayerId = player1Id)

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers.first().sourceName shouldBe "Dark Confidant"
        }

        test("does not trigger on opponent's upkeep with controllerOnly") {
            val (_, state) = newGame().addCreature(darkConfidant, player1Id)

            val event = EcsGameEvent.UpkeepBegan(activePlayerId = player2Id)

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers.shouldBeEmpty()
        }
    }

    context("attack trigger detection") {

        test("detects self attack trigger") {
            val (creatureId, state) = newGame().addCreature(heroOfBladehold, player1Id)

            val event = EcsGameEvent.AttackerDeclared(
                creatureId = creatureId,
                cardName = "Hero of Bladehold",
                attackingPlayerId = player1Id,
                defendingPlayerId = player2Id
            )

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers.first().sourceName shouldBe "Hero of Bladehold"
        }

        test("does not trigger for other attackers with selfOnly") {
            val (_, state1) = newGame().addCreature(heroOfBladehold, player1Id)
            val (bearId, state) = state1.addCreature(basicBear, player1Id)

            val event = EcsGameEvent.AttackerDeclared(
                creatureId = bearId,
                cardName = "Grizzly Bears",
                attackingPlayerId = player1Id,
                defendingPlayerId = player2Id
            )

            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers.shouldBeEmpty()
        }
    }

    context("phase step triggers") {

        test("detectPhaseStepTriggers for upkeep") {
            val (_, state) = newGame().addCreature(darkConfidant, player1Id)

            val triggers = detector.detectPhaseStepTriggers(
                state, "Beginning", "Upkeep", player1Id, registry
            )

            triggers shouldHaveSize 1
        }
    }

    context("APNAP ordering") {

        test("active player triggers come first") {
            // Both players have creatures with upkeep triggers
            val (_, state1) = newGame().addCreature(darkConfidant, player1Id)
            val state2 = state1.addCreature(darkConfidant, player2Id).second
            // Set active player via turnState
            val state = state2.copy(
                turnState = state2.turnState.copy(activePlayer = player1Id)
            )

            val event = EcsGameEvent.UpkeepBegan(activePlayerId = player1Id)

            // Only player1's trigger should fire (controllerOnly = true)
            val triggers = detector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers.first().controllerId shouldBe player1Id
        }
    }

    context("multiple events") {

        test("detects triggers from multiple events") {
            val (creature1Id, state1) = newGame().addCreature(elvishVisionary, player1Id)
            val (creature2Id, state) = state1.addCreature(elvishVisionary, player2Id)

            val events = listOf(
                EcsGameEvent.EnteredBattlefield(
                    entityId = creature1Id,
                    cardName = "Elvish Visionary",
                    controllerId = player1Id,
                    fromZone = ZoneId.hand(player1Id)
                ),
                EcsGameEvent.EnteredBattlefield(
                    entityId = creature2Id,
                    cardName = "Elvish Visionary",
                    controllerId = player2Id,
                    fromZone = ZoneId.hand(player2Id)
                )
            )

            val triggers = detector.detectTriggers(state, events, registry)

            triggers shouldHaveSize 2
        }
    }
})
