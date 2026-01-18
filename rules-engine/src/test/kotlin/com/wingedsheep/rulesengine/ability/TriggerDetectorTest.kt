package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.action.GameEvent
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.ManaSymbol
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize

class TriggerDetectorTest : FunSpec({

    // Helper functions
    fun createTestPlayer(id: String): Player {
        return Player.create(PlayerId.of(id), "${id.replaceFirstChar { it.uppercaseChar() }}'s Deck")
    }

    fun createCreatureDefinition(name: String): CardDefinition {
        return CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W)),
            subtypes = setOf(Subtype("Human")),
            power = 2,
            toughness = 2
        )
    }

    fun createCreatureInstance(name: String, controllerId: String): CardInstance {
        val definition = createCreatureDefinition(name)
        return CardInstance.create(definition, controllerId)
    }

    fun createTestGameState(player1: Player, player2: Player): GameState {
        return GameState.newGame(player1, player2)
    }

    context("CardMoved event triggers") {
        test("detects enter battlefield trigger for self") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Create a creature with ETB trigger
            val soulWardenDef = createCreatureDefinition("Soul Warden")
            val soulWarden = CardInstance.create(soulWardenDef, "player1")
            state = state.updateBattlefield { it.addToTop(soulWarden) }

            // Register the trigger
            val registry = AbilityRegistry()
            val etbAbility = TriggeredAbility.create(
                trigger = OnEnterBattlefield(selfOnly = true),
                effect = GainLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("Soul Warden", etbAbility)

            // Create the event
            val event = GameEvent.CardMoved(
                cardId = soulWarden.id.value,
                cardName = "Soul Warden",
                fromZone = ZoneType.HAND.name,
                toZone = ZoneType.BATTLEFIELD.name
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers[0].sourceName shouldBe "Soul Warden"
            triggers[0].controllerId shouldBe PlayerId.of("player1")
        }

        test("detects enter battlefield trigger for any creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Create Soul Warden with "whenever a creature ETB" trigger
            val soulWardenDef = createCreatureDefinition("Soul Warden")
            val soulWarden = CardInstance.create(soulWardenDef, "player1")
            state = state.updateBattlefield { it.addToTop(soulWarden) }

            // Create another creature entering
            val bearsDef = createCreatureDefinition("Grizzly Bears")
            val bears = CardInstance.create(bearsDef, "player2")
            state = state.updateBattlefield { it.addToTop(bears) }

            // Register the trigger
            val registry = AbilityRegistry()
            val etbAbility = TriggeredAbility.create(
                trigger = OnEnterBattlefield(selfOnly = false),
                effect = GainLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("Soul Warden", etbAbility)

            // Event: Grizzly Bears enters
            val event = GameEvent.CardMoved(
                cardId = bears.id.value,
                cardName = "Grizzly Bears",
                fromZone = ZoneType.HAND.name,
                toZone = ZoneType.BATTLEFIELD.name
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers[0].sourceName shouldBe "Soul Warden"
        }

        test("does not trigger when card moves to non-battlefield zone") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val soulWardenDef = createCreatureDefinition("Soul Warden")
            val soulWarden = CardInstance.create(soulWardenDef, "player1")
            state = state.updateBattlefield { it.addToTop(soulWarden) }

            val registry = AbilityRegistry()
            val etbAbility = TriggeredAbility.create(
                trigger = OnEnterBattlefield(selfOnly = false),
                effect = GainLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("Soul Warden", etbAbility)

            // Card moves to graveyard, not battlefield
            val event = GameEvent.CardMoved(
                cardId = "card_123",
                cardName = "Grizzly Bears",
                fromZone = ZoneType.HAND.name,
                toZone = ZoneType.GRAVEYARD.name
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 0
        }
    }

    context("CreatureDied event triggers") {
        test("detects death trigger for any creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Create Blood Artist with "whenever a creature dies" trigger
            val bloodArtistDef = createCreatureDefinition("Blood Artist")
            val bloodArtist = CardInstance.create(bloodArtistDef, "player1")
            state = state.updateBattlefield { it.addToTop(bloodArtist) }

            val registry = AbilityRegistry()
            val deathAbility = TriggeredAbility.create(
                trigger = OnDeath(selfOnly = false),
                effect = CompositeEffect(
                    effects = listOf(
                        LoseLifeEffect(amount = 1, target = EffectTarget.EachOpponent),
                        GainLifeEffect(amount = 1, target = EffectTarget.Controller)
                    )
                )
            )
            registry.registerTriggeredAbility("Blood Artist", deathAbility)

            // Event: some creature dies
            val event = GameEvent.CreatureDied(
                cardId = "other_creature",
                cardName = "Grizzly Bears",
                ownerId = "player2"
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers[0].sourceName shouldBe "Blood Artist"
        }
    }

    context("CardDrawn event triggers") {
        test("detects draw trigger for controller") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Create Jace's Erasure
            val jaceDef = createCreatureDefinition("Jace's Erasure")
            val jace = CardInstance.create(jaceDef, "player1")
            state = state.updateBattlefield { it.addToTop(jace) }

            val registry = AbilityRegistry()
            val drawAbility = TriggeredAbility.create(
                trigger = OnDraw(controllerOnly = true),
                effect = DiscardCardsEffect(count = 1, target = EffectTarget.Opponent)
            )
            registry.registerTriggeredAbility("Jace's Erasure", drawAbility)

            // Player 1 draws a card
            val event = GameEvent.CardDrawn(
                playerId = "player1",
                cardId = "card_drawn",
                cardName = "Island"
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers[0].sourceName shouldBe "Jace's Erasure"
        }

        test("does not trigger when opponent draws if controllerOnly") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val jaceDef = createCreatureDefinition("Jace's Erasure")
            val jace = CardInstance.create(jaceDef, "player1")
            state = state.updateBattlefield { it.addToTop(jace) }

            val registry = AbilityRegistry()
            val drawAbility = TriggeredAbility.create(
                trigger = OnDraw(controllerOnly = true),
                effect = DiscardCardsEffect(count = 1, target = EffectTarget.Opponent)
            )
            registry.registerTriggeredAbility("Jace's Erasure", drawAbility)

            // Player 2 draws a card
            val event = GameEvent.CardDrawn(
                playerId = "player2",
                cardId = "card_drawn",
                cardName = "Mountain"
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 0
        }
    }

    context("DamageDealt event triggers") {
        test("detects damage trigger") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Create Guttersnipe with "whenever deals damage" trigger
            val snipeDef = createCreatureDefinition("Guttersnipe")
            val snipe = CardInstance.create(snipeDef, "player1")
            state = state.updateBattlefield { it.addToTop(snipe) }

            val registry = AbilityRegistry()
            val damageAbility = TriggeredAbility.create(
                trigger = OnDealsDamage(selfOnly = true),
                effect = DealDamageEffect(amount = 2, target = EffectTarget.EachOpponent)
            )
            registry.registerTriggeredAbility("Guttersnipe", damageAbility)

            // Guttersnipe deals damage
            val event = GameEvent.DamageDealt(
                sourceId = snipe.id.value,
                targetId = "player2",
                amount = 2,
                isPlayer = true
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            triggers[0].sourceName shouldBe "Guttersnipe"
        }
    }

    context("Phase/Step triggers") {
        test("detects upkeep trigger for controller") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Create Bitterblossom with upkeep trigger
            val blossomDef = createCreatureDefinition("Bitterblossom")
            val blossom = CardInstance.create(blossomDef, "player1")
            state = state.updateBattlefield { it.addToTop(blossom) }

            val registry = AbilityRegistry()
            val upkeepAbility = TriggeredAbility.create(
                trigger = OnUpkeep(controllerOnly = true),
                effect = LoseLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("Bitterblossom", upkeepAbility)

            val triggers = TriggerDetector.detectPhaseStepTriggers(
                state,
                Phase.BEGINNING,
                Step.UPKEEP,
                registry
            )

            triggers shouldHaveSize 1
            triggers[0].sourceName shouldBe "Bitterblossom"
        }

        test("does not trigger upkeep on opponent's turn if controllerOnly") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Bitterblossom controlled by player2
            val blossomDef = createCreatureDefinition("Bitterblossom")
            val blossom = CardInstance.create(blossomDef, "player2")
            state = state.updateBattlefield { it.addToTop(blossom) }

            val registry = AbilityRegistry()
            val upkeepAbility = TriggeredAbility.create(
                trigger = OnUpkeep(controllerOnly = true),
                effect = LoseLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("Bitterblossom", upkeepAbility)

            // It's player1's turn
            val triggers = TriggerDetector.detectPhaseStepTriggers(
                state,
                Phase.BEGINNING,
                Step.UPKEEP,
                registry
            )

            triggers shouldHaveSize 0
        }

        test("detects end step trigger") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creatureDef = createCreatureDefinition("End-Step Trigger")
            val creature = CardInstance.create(creatureDef, "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val registry = AbilityRegistry()
            val endStepAbility = TriggeredAbility.create(
                trigger = OnEndStep(controllerOnly = true),
                effect = DrawCardsEffect(count = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("End-Step Trigger", endStepAbility)

            val triggers = TriggerDetector.detectPhaseStepTriggers(
                state,
                Phase.ENDING,
                Step.END,
                registry
            )

            triggers shouldHaveSize 1
        }
    }

    context("APNAP ordering") {
        test("active player triggers come before non-active player triggers") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Both players have creatures with ETB triggers
            val creature1Def = createCreatureDefinition("Creature A")
            val creature1 = CardInstance.create(creature1Def, "player1")
            val creature2Def = createCreatureDefinition("Creature B")
            val creature2 = CardInstance.create(creature2Def, "player2")

            state = state
                .updateBattlefield { it.addToTop(creature1) }
                .updateBattlefield { it.addToTop(creature2) }

            val registry = AbilityRegistry()
            val etbAbility = TriggeredAbility.create(
                trigger = OnEnterBattlefield(selfOnly = false),
                effect = GainLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("Creature A", etbAbility)
            registry.registerTriggeredAbility("Creature B", etbAbility)

            // A third creature enters
            val enteringCreatureDef = createCreatureDefinition("Entering Creature")
            val enteringCreature = CardInstance.create(enteringCreatureDef, "player1")
            state = state.updateBattlefield { it.addToTop(enteringCreature) }

            val event = GameEvent.CardMoved(
                cardId = enteringCreature.id.value,
                cardName = "Entering Creature",
                fromZone = ZoneType.HAND.name,
                toZone = ZoneType.BATTLEFIELD.name
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 2
            // Active player (player1) triggers should come first
            triggers[0].controllerId shouldBe PlayerId.of("player1")
            triggers[1].controllerId shouldBe PlayerId.of("player2")
        }
    }

    context("Multiple events") {
        test("detects triggers from multiple events") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Create two permanents with different triggers
            val creature1Def = createCreatureDefinition("ETB Creature")
            val creature1 = CardInstance.create(creature1Def, "player1")
            val creature2Def = createCreatureDefinition("Death Creature")
            val creature2 = CardInstance.create(creature2Def, "player1")

            state = state
                .updateBattlefield { it.addToTop(creature1) }
                .updateBattlefield { it.addToTop(creature2) }

            val registry = AbilityRegistry()
            val etbAbility = TriggeredAbility.create(
                trigger = OnEnterBattlefield(selfOnly = false),
                effect = GainLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            val deathAbility = TriggeredAbility.create(
                trigger = OnDeath(selfOnly = false),
                effect = DrawCardsEffect(count = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("ETB Creature", etbAbility)
            registry.registerTriggeredAbility("Death Creature", deathAbility)

            val events = listOf(
                GameEvent.CardMoved(
                    cardId = "new_creature",
                    cardName = "New Creature",
                    fromZone = ZoneType.HAND.name,
                    toZone = ZoneType.BATTLEFIELD.name
                ),
                GameEvent.CreatureDied(
                    cardId = "dying_creature",
                    cardName = "Dying Creature",
                    ownerId = "player2"
                )
            )

            val triggers = TriggerDetector.detectTriggers(state, events, registry)

            triggers shouldHaveSize 2
        }
    }

    context("TriggerContext") {
        test("ZoneChange context stores correct information") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creatureDef = createCreatureDefinition("Panharmonicon")
            val creature = CardInstance.create(creatureDef, "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val registry = AbilityRegistry()
            val etbAbility = TriggeredAbility.create(
                trigger = OnEnterBattlefield(selfOnly = false),
                effect = GainLifeEffect(amount = 1, target = EffectTarget.Controller)
            )
            registry.registerTriggeredAbility("Panharmonicon", etbAbility)

            val event = GameEvent.CardMoved(
                cardId = "entering_card_123",
                cardName = "Grizzly Bears",
                fromZone = ZoneType.HAND.name,
                toZone = ZoneType.BATTLEFIELD.name
            )

            val triggers = TriggerDetector.detectTriggers(state, listOf(event), registry)

            triggers shouldHaveSize 1
            val context = triggers[0].triggerContext as TriggerContext.ZoneChange
            context.cardId shouldBe CardId("entering_card_123")
            context.cardName shouldBe "Grizzly Bears"
            context.fromZone shouldBe ZoneType.HAND.name
            context.toZone shouldBe ZoneType.BATTLEFIELD.name
        }
    }
})
