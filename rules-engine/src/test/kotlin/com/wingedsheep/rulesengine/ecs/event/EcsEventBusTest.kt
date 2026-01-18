package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.action.EcsActionEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class EcsEventBusTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    context("event publishing and subscription") {

        test("subscribers receive published events") {
            val bus = EcsEventBus()
            val receivedEvents = mutableListOf<EcsGameEvent>()
            val state = newGame()

            bus.subscribe(object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    receivedEvents.add(event)
                }
            })

            val event = EcsGameEvent.LifeGained(player1Id, 5, 25)
            bus.publish(event, state)

            receivedEvents shouldHaveSize 1
            receivedEvents.first() shouldBe event
        }

        test("multiple subscribers all receive events") {
            val bus = EcsEventBus()
            val received1 = mutableListOf<EcsGameEvent>()
            val received2 = mutableListOf<EcsGameEvent>()
            val state = newGame()

            bus.subscribe(object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    received1.add(event)
                }
            })

            bus.subscribe(object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    received2.add(event)
                }
            })

            val event = EcsGameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest")
            bus.publish(event, state)

            received1 shouldHaveSize 1
            received2 shouldHaveSize 1
        }

        test("unsubscribed listeners don't receive events") {
            val bus = EcsEventBus()
            val receivedEvents = mutableListOf<EcsGameEvent>()
            val state = newGame()

            val subscriber = object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    receivedEvents.add(event)
                }
            }

            bus.subscribe(subscriber)
            bus.unsubscribe(subscriber)

            bus.publish(EcsGameEvent.LifeGained(player1Id, 5, 25), state)

            receivedEvents.shouldBeEmpty()
        }
    }

    context("typed subscriptions") {

        test("typed subscriber only receives matching event type") {
            val bus = EcsEventBus()
            val state = newGame()
            val lifeEvents = mutableListOf<EcsGameEvent.LifeGained>()

            bus.subscribe<EcsGameEvent.LifeGained> { event, _ ->
                lifeEvents.add(event)
            }

            // Publish multiple event types
            bus.publish(EcsGameEvent.LifeGained(player1Id, 5, 25), state)
            bus.publish(EcsGameEvent.LifeLost(player1Id, 3, 17), state)
            bus.publish(EcsGameEvent.LifeGained(player2Id, 2, 22), state)

            lifeEvents shouldHaveSize 2
            lifeEvents.all { it is EcsGameEvent.LifeGained } shouldBe true
        }
    }

    context("publishAll") {

        test("publishes multiple events") {
            val bus = EcsEventBus()
            val receivedEvents = mutableListOf<EcsGameEvent>()
            val state = newGame()

            bus.subscribe(object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    receivedEvents.add(event)
                }
            })

            val events = listOf(
                EcsGameEvent.LifeGained(player1Id, 5, 25),
                EcsGameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest"),
                EcsGameEvent.UpkeepBegan(player1Id)
            )

            bus.publishAll(events, state)

            receivedEvents shouldHaveSize 3
        }
    }

    context("publishActionEvents") {

        test("converts and publishes action events") {
            val bus = EcsEventBus()
            val receivedEvents = mutableListOf<EcsGameEvent>()
            val state = newGame()

            bus.subscribe(object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    receivedEvents.add(event)
                }
            })

            val actionEvents = listOf(
                EcsActionEvent.LifeChanged(player1Id, 20, 25),  // +5 life
                EcsActionEvent.CardDrawn(player1Id, EntityId.of("card1"), "Island")
            )

            bus.publishActionEvents(actionEvents, state)

            receivedEvents shouldHaveSize 2
            receivedEvents.any { it is EcsGameEvent.LifeGained } shouldBe true
            receivedEvents.any { it is EcsGameEvent.CardDrawn } shouldBe true
        }

        test("action events that don't map to game events don't produce events") {
            val bus = EcsEventBus()
            val receivedEvents = mutableListOf<EcsGameEvent>()
            val state = newGame()

            bus.subscribe(object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    receivedEvents.add(event)
                }
            })

            // ManaAdded doesn't map to a game event
            val actionEvents = listOf(
                EcsActionEvent.ManaAdded(player1Id, "Green", 1)
            )

            bus.publishActionEvents(actionEvents, state)

            receivedEvents.shouldBeEmpty()
        }
    }

    context("event history") {

        test("records event history") {
            val bus = EcsEventBus()
            val state = newGame()

            bus.publish(EcsGameEvent.LifeGained(player1Id, 5, 25), state)
            bus.publish(EcsGameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest"), state)

            val history = bus.getEventHistory()

            history shouldHaveSize 2
            history[0].event shouldBe EcsGameEvent.LifeGained(player1Id, 5, 25)
            history[1].event shouldBe EcsGameEvent.CardDrawn(player1Id, EntityId.of("card1"), "Forest")
        }

        test("getRecentEvents returns limited events") {
            val bus = EcsEventBus()
            val state = newGame()

            repeat(10) { i ->
                bus.publish(EcsGameEvent.LifeGained(player1Id, i, 20 + i), state)
            }

            val recent = bus.getRecentEvents(3)

            recent shouldHaveSize 3
        }

        test("clearHistory removes all events") {
            val bus = EcsEventBus()
            val state = newGame()

            bus.publish(EcsGameEvent.LifeGained(player1Id, 5, 25), state)
            bus.clearHistory()

            bus.getEventHistory().shouldBeEmpty()
        }
    }

    context("clearSubscribers") {

        test("removes all subscribers") {
            val bus = EcsEventBus()
            val receivedEvents = mutableListOf<EcsGameEvent>()
            val state = newGame()

            bus.subscribe(object : EcsEventSubscriber {
                override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
                    receivedEvents.add(event)
                }
            })

            bus.clearSubscribers()
            bus.publish(EcsGameEvent.LifeGained(player1Id, 5, 25), state)

            receivedEvents.shouldBeEmpty()
        }
    }

    context("TriggerCollector") {

        test("collects triggers from events") {
            // This test would need a full setup with abilities
            // For now, just verify the basic API works
            val detector = EcsTriggerDetector()
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

            logger.onEvent(EcsGameEvent.LifeGained(player1Id, 5, 25), state)

            logged shouldHaveSize 1
            logged.first() shouldContain "gained"
            logged.first() shouldContain "5"
        }

        test("formats different event types") {
            val logged = mutableListOf<String>()
            val logger = EventLogger { logged.add(it) }
            val state = newGame()

            logger.onEvent(EcsGameEvent.CreatureDied(EntityId.of("bear"), "Grizzly Bears", player1Id), state)
            logger.onEvent(EcsGameEvent.CardDrawn(player1Id, EntityId.of("card"), "Forest"), state)
            logger.onEvent(EcsGameEvent.CombatBegan(player1Id, player2Id), state)

            logged shouldHaveSize 3
            logged[0] shouldContain "Grizzly Bears"
            logged[0] shouldContain "died"
            logged[1] shouldContain "drew"
            logged[2] shouldContain "Combat began"
        }
    }
})
