package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.action.EcsActionEvent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import kotlin.reflect.KClass

/**
 * Event bus for the ECS game system.
 *
 * Provides event publishing and subscription capabilities for game events.
 * Used to detect triggers, log game events, and notify UI systems.
 *
 * The event bus is designed to work with immutable game state - subscribers
 * receive events but cannot directly modify state. Instead, they can return
 * responses (like triggered abilities) that the game engine processes.
 */
class EcsEventBus {

    private val subscribers = mutableListOf<EcsEventSubscriber>()
    private val typedSubscribers = mutableMapOf<KClass<out EcsGameEvent>, MutableList<EcsTypedEventSubscriber<*>>>()
    private val eventHistory = mutableListOf<TimestampedEvent>()

    /**
     * Subscribe to all events.
     */
    fun subscribe(subscriber: EcsEventSubscriber) {
        subscribers.add(subscriber)
    }

    /**
     * Subscribe to events of a specific type.
     */
    inline fun <reified T : EcsGameEvent> subscribe(noinline handler: (T, EcsGameState) -> Unit) {
        subscribeTyped(T::class, object : EcsTypedEventSubscriber<T> {
            override fun onEvent(event: T, state: EcsGameState) {
                handler(event, state)
            }
        })
    }

    /**
     * Subscribe to a specific event type with a typed subscriber.
     */
    fun <T : EcsGameEvent> subscribeTyped(eventClass: KClass<T>, subscriber: EcsTypedEventSubscriber<T>) {
        typedSubscribers.getOrPut(eventClass) { mutableListOf() }.add(subscriber)
    }

    /**
     * Unsubscribe from all events.
     */
    fun unsubscribe(subscriber: EcsEventSubscriber) {
        subscribers.remove(subscriber)
    }

    /**
     * Publish a game event.
     */
    fun publish(event: EcsGameEvent, state: EcsGameState) {
        // Record in history
        eventHistory.add(TimestampedEvent(System.currentTimeMillis(), event))

        // Notify all subscribers
        subscribers.forEach { it.onEvent(event, state) }

        // Notify typed subscribers
        @Suppress("UNCHECKED_CAST")
        typedSubscribers[event::class]?.forEach { subscriber ->
            (subscriber as EcsTypedEventSubscriber<EcsGameEvent>).onEvent(event, state)
        }
    }

    /**
     * Publish multiple game events.
     */
    fun publishAll(events: List<EcsGameEvent>, state: EcsGameState) {
        events.forEach { publish(it, state) }
    }

    /**
     * Publish events from an action result.
     */
    fun publishActionEvents(actionEvents: List<EcsActionEvent>, state: EcsGameState) {
        for (actionEvent in actionEvents) {
            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)
            publishAll(gameEvents, state)
        }
    }

    /**
     * Publish events from effect execution.
     */
    fun publishEffectEvents(effectEvents: List<EcsEvent>, state: EcsGameState) {
        for (effectEvent in effectEvents) {
            val gameEvents = EcsGameEventConverter.fromEffectEvent(effectEvent)
            publishAll(gameEvents, state)
        }
    }

    /**
     * Get all events in history.
     */
    fun getEventHistory(): List<TimestampedEvent> = eventHistory.toList()

    /**
     * Get recent events (within the last N events).
     */
    fun getRecentEvents(count: Int): List<EcsGameEvent> =
        eventHistory.takeLast(count).map { it.event }

    /**
     * Clear event history.
     */
    fun clearHistory() {
        eventHistory.clear()
    }

    /**
     * Clear all subscribers.
     */
    fun clearSubscribers() {
        subscribers.clear()
        typedSubscribers.clear()
    }
}

/**
 * Subscriber that receives all game events.
 */
interface EcsEventSubscriber {
    fun onEvent(event: EcsGameEvent, state: EcsGameState)
}

/**
 * Typed subscriber for specific event types.
 */
interface EcsTypedEventSubscriber<T : EcsGameEvent> {
    fun onEvent(event: T, state: EcsGameState)
}

/**
 * An event with its timestamp.
 */
data class TimestampedEvent(
    val timestamp: Long,
    val event: EcsGameEvent
)

/**
 * Trigger-aware event subscriber that collects pending triggers.
 *
 * Use this subscriber with an event bus to automatically detect
 * triggered abilities as events occur.
 */
class TriggerCollector(
    private val triggerDetector: EcsTriggerDetector,
    private val abilityRegistry: EcsAbilityRegistry
) : EcsEventSubscriber {

    private val pendingTriggers = mutableListOf<EcsPendingTrigger>()

    override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
        val triggers = triggerDetector.detectTriggers(state, listOf(event), abilityRegistry)
        pendingTriggers.addAll(triggers)
    }

    /**
     * Get and clear all pending triggers.
     */
    fun drainPendingTriggers(): List<EcsPendingTrigger> {
        val result = pendingTriggers.toList()
        pendingTriggers.clear()
        return result
    }

    /**
     * Peek at pending triggers without clearing.
     */
    fun peekPendingTriggers(): List<EcsPendingTrigger> = pendingTriggers.toList()

    /**
     * Check if there are any pending triggers.
     */
    fun hasPendingTriggers(): Boolean = pendingTriggers.isNotEmpty()

    /**
     * Clear all pending triggers.
     */
    fun clear() {
        pendingTriggers.clear()
    }
}

/**
 * Event logger subscriber for debugging and replay.
 */
class EventLogger(
    private val logFn: (String) -> Unit = ::println
) : EcsEventSubscriber {

    override fun onEvent(event: EcsGameEvent, state: EcsGameState) {
        logFn(formatEvent(event))
    }

    private fun formatEvent(event: EcsGameEvent): String {
        return when (event) {
            is EcsGameEvent.EnteredBattlefield -> "${event.cardName} entered the battlefield"
            is EcsGameEvent.LeftBattlefield -> "${event.cardName} left the battlefield"
            is EcsGameEvent.CreatureDied -> "${event.cardName} died"
            is EcsGameEvent.CardExiled -> "${event.cardName} was exiled"
            is EcsGameEvent.ReturnedToHand -> "${event.cardName} returned to hand"
            is EcsGameEvent.CardDrawn -> "Player drew ${event.cardName}"
            is EcsGameEvent.CardDiscarded -> "Player discarded ${event.cardName}"
            is EcsGameEvent.CombatBegan -> "Combat began"
            is EcsGameEvent.AttackerDeclared -> "${event.cardName} is attacking"
            is EcsGameEvent.BlockerDeclared -> "${event.blockerName} is blocking"
            is EcsGameEvent.CombatEnded -> "Combat ended"
            is EcsGameEvent.DamageDealtToPlayer -> "${event.amount} damage dealt to player"
            is EcsGameEvent.DamageDealtToCreature -> "${event.amount} damage dealt to creature"
            is EcsGameEvent.LifeGained -> "Player gained ${event.amount} life (now ${event.newTotal})"
            is EcsGameEvent.LifeLost -> "Player lost ${event.amount} life (now ${event.newTotal})"
            is EcsGameEvent.UpkeepBegan -> "Upkeep began"
            is EcsGameEvent.EndStepBegan -> "End step began"
            is EcsGameEvent.SpellCast -> "${event.spellName} was cast"
            is EcsGameEvent.PermanentTapped -> "${event.cardName} was tapped"
            is EcsGameEvent.PermanentUntapped -> "${event.cardName} was untapped"
            is EcsGameEvent.CountersAdded -> "${event.count} ${event.counterType} counter(s) added"
            is EcsGameEvent.CountersRemoved -> "${event.count} ${event.counterType} counter(s) removed"
            is EcsGameEvent.PlayerLost -> "Player lost: ${event.reason}"
            is EcsGameEvent.GameEnded -> "Game ended. Winner: ${event.winnerId ?: "None (draw)"}"
        }
    }
}
