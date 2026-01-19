package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class EventBusTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    context("event publishing and subscription") {

        test("subscribers receive published events") {
            val bus = EventBus()
            val receivedEvents = mutableListOf<GameEvent>()
            val state = newGame()

            bus.subscribe(object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    receivedEvents.add(event)
                }
            })

            val event = GameEvent.LifeGained(player1Id, 5, 25)
            bus.publish(event, state)

            receivedEvents shouldHaveSize 1
            receivedEvents.first() shouldBe event
        }

        test("multiple subscribers all receive events") {
            val bus = EventBus()
            val received1 = mutableListOf<GameEvent>()
            val received2 = mutableListOf<GameEvent>()
            val state = newGame()

            bus.subscribe(object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    received1.add(event)
                }
            })

            bus.subscribe(object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    received2.add(event)
                }
            })

            val event = GameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest")
            bus.publish(event, state)

            received1 shouldHaveSize 1
            received2 shouldHaveSize 1
        }

        test("unsubscribed listeners don't receive events") {
            val bus = EventBus()
            val receivedEvents = mutableListOf<GameEvent>()
            val state = newGame()

            val subscriber = object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    receivedEvents.add(event)
                }
            }

            bus.subscribe(subscriber)
            bus.unsubscribe(subscriber)

            bus.publish(GameEvent.LifeGained(player1Id, 5, 25), state)

            receivedEvents.shouldBeEmpty()
        }
    }

    context("typed subscriptions") {

        test("typed subscriber only receives matching event type") {
            val bus = EventBus()
            val state = newGame()
            val lifeEvents = mutableListOf<GameEvent.LifeGained>()

            bus.subscribe<GameEvent.LifeGained> { event, _ ->
                lifeEvents.add(event)
            }

            // Publish multiple event types
            bus.publish(GameEvent.LifeGained(player1Id, 5, 25), state)
            bus.publish(GameEvent.LifeLost(player1Id, 3, 17), state)
            bus.publish(GameEvent.LifeGained(player2Id, 2, 22), state)

            lifeEvents shouldHaveSize 2
            lifeEvents.all { it is GameEvent.LifeGained } shouldBe true
        }
    }

    context("publishAll") {

        test("publishes multiple events") {
            val bus = EventBus()
            val receivedEvents = mutableListOf<GameEvent>()
            val state = newGame()

            bus.subscribe(object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    receivedEvents.add(event)
                }
            })

            val events = listOf(
                GameEvent.LifeGained(player1Id, 5, 25),
                GameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest"),
                GameEvent.UpkeepBegan(player1Id)
            )

            bus.publishAll(events, state)

            receivedEvents shouldHaveSize 3
        }
    }

    context("publishActionEvents") {

        test("converts and publishes action events") {
            val bus = EventBus()
            val receivedEvents = mutableListOf<GameEvent>()
            val state = newGame()

            bus.subscribe(object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    receivedEvents.add(event)
                }
            })

            val actionEvents = listOf(
                GameActionEvent.LifeChanged(player1Id, 20, 25),  // +5 life
                GameActionEvent.CardDrawn(player1Id, EntityId.of("card1"), "Island")
            )

            bus.publishActionEvents(actionEvents, state)

            receivedEvents shouldHaveSize 2
            receivedEvents.any { it is GameEvent.LifeGained } shouldBe true
            receivedEvents.any { it is GameEvent.CardDrawn } shouldBe true
        }

        test("action events that don't map to game events don't produce events") {
            val bus = EventBus()
            val receivedEvents = mutableListOf<GameEvent>()
            val state = newGame()

            bus.subscribe(object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    receivedEvents.add(event)
                }
            })

            // ManaAdded doesn't map to a game event
            val actionEvents = listOf(
                GameActionEvent.ManaAdded(player1Id, "Green", 1)
            )

            bus.publishActionEvents(actionEvents, state)

            receivedEvents.shouldBeEmpty()
        }
    }

    context("event history") {

        test("records event history") {
            val bus = EventBus()
            val state = newGame()

            bus.publish(GameEvent.LifeGained(player1Id, 5, 25), state)
            bus.publish(GameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest"), state)

            val history = bus.getEventHistory()

            history shouldHaveSize 2
            history[0].event shouldBe GameEvent.LifeGained(player1Id, 5, 25)
            history[1].event shouldBe GameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest")
        }

        test("getRecentEvents returns limited events") {
            val bus = EventBus()
            val state = newGame()

            repeat(10) { i ->
                bus.publish(GameEvent.LifeGained(player1Id, i, 20 + i), state)
            }

            val recent = bus.getRecentEvents(3)

            recent shouldHaveSize 3
        }

        test("clearHistory removes all events") {
            val bus = EventBus()
            val state = newGame()

            bus.publish(GameEvent.LifeGained(player1Id, 5, 25), state)
            bus.clearHistory()

            bus.getEventHistory().shouldBeEmpty()
        }
    }

    context("clearSubscribers") {

        test("removes all subscribers") {
            val bus = EventBus()
            val receivedEvents = mutableListOf<GameEvent>()
            val state = newGame()

            bus.subscribe(object : EventSubscriber {
                override fun onEvent(event: GameEvent, state: GameState) {
                    receivedEvents.add(event)
                }
            })

            bus.clearSubscribers()
            bus.publish(GameEvent.LifeGained(player1Id, 5, 25), state)

            receivedEvents.shouldBeEmpty()
        }
    }

    context("TriggerCollector") {

        test("collects triggers from events") {
            // This test would need a full setup with abilities
            // For now, just verify the basic API works
            val detector = TriggerDetector()
            val registry = CardDefinitionAbilityRegistry()
            val collector = TriggerCollector(detector, registry)

            collector.hasPendingTriggers() shouldBe false
            collector.drainPendingTriggers().shouldBeEmpty()
        }
    }

    context("EventLogger") {

        test("logs events") {
            val logged = mutableListOf<String>()
            val logger = EventLogger { logged.add(it) }
            val state = newGame()

            logger.onEvent(GameEvent.LifeGained(player1Id, 5, 25), state)

            logged shouldHaveSize 1
            logged.first() shouldContain "gained"
            logged.first() shouldContain "5"
        }

        test("formats different event types") {
            val logged = mutableListOf<String>()
            val logger = EventLogger { logged.add(it) }
            val state = newGame()

            logger.onEvent(GameEvent.CreatureDied(EntityId.of("bear"), "Grizzly Bears", player1Id), state)
            logger.onEvent(GameEvent.CardDrawn(player1Id, EntityId.of("card"), "Forest"), state)
            logger.onEvent(GameEvent.CombatBegan(player1Id, player2Id), state)

            logged shouldHaveSize 3
            logged[0] shouldContain "Grizzly Bears"
            logged[0] shouldContain "died"
            logged[1] shouldContain "drew"
            logged[2] shouldContain "Combat began"
        }
    }
})
