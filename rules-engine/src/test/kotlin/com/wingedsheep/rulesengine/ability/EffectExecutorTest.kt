package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.action.GameEvent
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.ManaSymbol
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize

class EffectExecutorTest : FunSpec({

    // Helper functions
    fun createTestPlayer(id: String): Player {
        return Player.create(PlayerId.of(id), "${id.replaceFirstChar { it.uppercaseChar() }}'s Deck")
    }

    fun createCreatureDefinition(name: String, power: Int = 2, toughness: Int = 2): CardDefinition {
        return CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W)),
            subtypes = setOf(Subtype("Human")),
            power = power,
            toughness = toughness
        )
    }

    fun createCreatureInstance(name: String, controllerId: String): CardInstance {
        val definition = createCreatureDefinition(name)
        return CardInstance.create(definition, controllerId)
    }

    fun createTestGameState(player1: Player, player2: Player): GameState {
        return GameState.newGame(player1, player2)
    }

    context("GainLifeEffect") {
        test("controller gains life") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = GainLifeEffect(amount = 3, target = EffectTarget.Controller)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player1")).life shouldBe 23
            events shouldHaveSize 1
            (events[0] as GameEvent.LifeChanged).delta shouldBe 3
        }

        test("opponent gains life") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = GainLifeEffect(amount = 5, target = EffectTarget.Opponent)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player2")).life shouldBe 25
        }
    }

    context("LoseLifeEffect") {
        test("opponent loses life") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = LoseLifeEffect(amount = 3, target = EffectTarget.Opponent)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player2")).life shouldBe 17
            events shouldHaveSize 1
            (events[0] as GameEvent.LifeChanged).delta shouldBe -3
        }

        test("controller loses life") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = LoseLifeEffect(amount = 2, target = EffectTarget.Controller)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player1")).life shouldBe 18
        }
    }

    context("DealDamageEffect") {
        test("deals damage to opponent") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = DealDamageEffect(amount = 3, target = EffectTarget.Opponent)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player2")).life shouldBe 17
            events.any { it is GameEvent.DamageDealt } shouldBe true
        }

        test("deals damage to each opponent") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = DealDamageEffect(amount = 2, target = EffectTarget.EachOpponent)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player2")).life shouldBe 18
        }

        test("deals damage to target creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Grizzly Bears", "player2")
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = DealDamageEffect(amount = 2, target = EffectTarget.TargetCreature)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(creature.id)),
                events
            )

            val damagedCreature = newState.battlefield.getCard(creature.id)!!
            damagedCreature.damageMarked shouldBe 2
        }

        test("deals damage to chosen player target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = DealDamageEffect(amount = 3, target = EffectTarget.AnyTarget)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.PlayerTarget(PlayerId.of("player2"))),
                events
            )

            newState.getPlayer(PlayerId.of("player2")).life shouldBe 17
        }
    }

    context("DrawCardsEffect") {
        test("controller draws cards") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Add cards to library
            val card1 = createCreatureInstance("Card 1", "player1")
            val card2 = createCreatureInstance("Card 2", "player1")
            state = state.updatePlayer(PlayerId.of("player1")) { p ->
                p.copy(library = p.library.addToTop(card1).addToTop(card2))
            }

            val events = mutableListOf<GameEvent>()
            val effect = DrawCardsEffect(count = 2, target = EffectTarget.Controller)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player1")).hand.size shouldBe 2
            events.filterIsInstance<GameEvent.CardDrawn>() shouldHaveSize 2
        }
    }

    context("DiscardCardsEffect") {
        test("opponent discards cards") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            // Add cards to hand
            val card1 = createCreatureInstance("Card 1", "player2")
            val card2 = createCreatureInstance("Card 2", "player2")
            state = state.updatePlayer(PlayerId.of("player2")) { p ->
                p.copy(hand = p.hand.addToTop(card1).addToTop(card2))
            }

            val events = mutableListOf<GameEvent>()
            val effect = DiscardCardsEffect(count = 1, target = EffectTarget.Opponent)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player2")).hand.size shouldBe 1
            newState.getPlayer(PlayerId.of("player2")).graveyard.size shouldBe 1
        }
    }

    context("DestroyEffect") {
        test("destroys target creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Grizzly Bears", "player2")
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = DestroyEffect(target = EffectTarget.TargetCreature)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(creature.id)),
                events
            )

            newState.battlefield.getCard(creature.id) shouldBe null
            newState.getPlayer(PlayerId.of("player2")).graveyard.size shouldBe 1
            events.any { it is GameEvent.CardMoved } shouldBe true
            events.any { it is GameEvent.CreatureDied } shouldBe true
        }
    }

    context("TapUntapEffect") {
        test("taps target creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Grizzly Bears", "player2")
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = TapUntapEffect(tap = true, target = EffectTarget.TargetCreature)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(creature.id)),
                events
            )

            newState.battlefield.getCard(creature.id)!!.isTapped shouldBe true
            events.any { it is GameEvent.CardTapped } shouldBe true
        }

        test("untaps target creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Grizzly Bears", "player2").tap()
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = TapUntapEffect(tap = false, target = EffectTarget.TargetCreature)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(creature.id)),
                events
            )

            newState.battlefield.getCard(creature.id)!!.isTapped shouldBe false
            events.any { it is GameEvent.CardUntapped } shouldBe true
        }
    }

    context("ModifyStatsEffect") {
        test("modifies creature stats") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Grizzly Bears", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = ModifyStatsEffect(
                powerModifier = 2,
                toughnessModifier = 2,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(creature.id)),
                events
            )

            val modifiedCreature = newState.battlefield.getCard(creature.id)!!
            modifiedCreature.currentPower shouldBe 4
            modifiedCreature.currentToughness shouldBe 4
        }

        test("applies negative modifiers") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Grizzly Bears", "player2")
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = ModifyStatsEffect(
                powerModifier = -1,
                toughnessModifier = -1,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(creature.id)),
                events
            )

            val modifiedCreature = newState.battlefield.getCard(creature.id)!!
            modifiedCreature.currentPower shouldBe 1
            modifiedCreature.currentToughness shouldBe 1
        }
    }

    context("AddCountersEffect") {
        test("adds counters to target creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Grizzly Bears", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = AddCountersEffect(
                counterType = "+1/+1",
                count = 2,
                target = EffectTarget.TargetCreature
            )
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(creature.id)),
                events
            )

            val modifiedCreature = newState.battlefield.getCard(creature.id)!!
            modifiedCreature.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
        }

        test("adds counters to self") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreatureInstance("Walking Ballista", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val events = mutableListOf<GameEvent>()
            val effect = AddCountersEffect(
                counterType = "+1/+1",
                count = 1,
                target = EffectTarget.Self
            )
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                creature.id,
                emptyList(),
                events
            )

            val modifiedCreature = newState.battlefield.getCard(creature.id)!!
            modifiedCreature.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 1
        }
    }

    context("AddManaEffect") {
        test("adds colored mana") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = AddManaEffect(color = Color.GREEN, amount = 2)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player1")).manaPool.get(Color.GREEN) shouldBe 2
            events.any { it is GameEvent.ManaAdded } shouldBe true
        }
    }

    context("AddColorlessManaEffect") {
        test("adds colorless mana") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = AddColorlessManaEffect(amount = 3)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player1")).manaPool.colorless shouldBe 3
        }
    }

    context("CompositeEffect") {
        test("executes multiple effects in order") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = CompositeEffect(
                effects = listOf(
                    GainLifeEffect(amount = 2, target = EffectTarget.Controller),
                    LoseLifeEffect(amount = 1, target = EffectTarget.Opponent)
                )
            )
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(),
                events
            )

            newState.getPlayer(PlayerId.of("player1")).life shouldBe 22
            newState.getPlayer(PlayerId.of("player2")).life shouldBe 19
        }
    }

    context("Edge cases") {
        test("effect with no valid target does nothing") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = DealDamageEffect(amount = 3, target = EffectTarget.AnyTarget)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                emptyList(), // No target chosen
                events
            )

            // State should be unchanged
            newState.getPlayer(PlayerId.of("player1")).life shouldBe 20
            newState.getPlayer(PlayerId.of("player2")).life shouldBe 20
        }

        test("destroy effect with invalid target does nothing") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)
            val events = mutableListOf<GameEvent>()

            val effect = DestroyEffect(target = EffectTarget.TargetCreature)
            val newState = EffectExecutor.execute(
                state,
                effect,
                PlayerId.of("player1"),
                CardId("source"),
                listOf(ChosenTarget.CardTarget(CardId("nonexistent"))),
                events
            )

            // State should be unchanged
            newState shouldBe state
        }
    }
})
