package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
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
class EventBus {

    private val subscribers = mutableListOf<EventSubscriber>()
    private val typedSubscribers = mutableMapOf<KClass<out GameEvent>, MutableList<TypedEventSubscriber<*>>>()
    private val eventHistory = mutableListOf<TimestampedEvent>()

    /**
     * Subscribe to all events.
     */
    fun subscribe(subscriber: EventSubscriber) {
        subscribers.add(subscriber)
    }

    /**
     * Subscribe to events of a specific type.
     */
    inline fun <reified T : GameEvent> subscribe(noinline handler: (T, GameState) -> Unit) {
        subscribeTyped(T::class, object : TypedEventSubscriber<T> {
            override fun onEvent(event: T, state: GameState) {
                handler(event, state)
            }
        })
    }

    /**
     * Subscribe to a specific event type with a typed subscriber.
     */
    fun <T : GameEvent> subscribeTyped(eventClass: KClass<T>, subscriber: TypedEventSubscriber<T>) {
        typedSubscribers.getOrPut(eventClass) { mutableListOf() }.add(subscriber)
    }

    /**
     * Unsubscribe from all events.
     */
    fun unsubscribe(subscriber: EventSubscriber) {
        subscribers.remove(subscriber)
    }

    /**
     * Publish a game event.
     */
    fun publish(event: GameEvent, state: GameState) {
        // Record in history
        eventHistory.add(TimestampedEvent(System.currentTimeMillis(), event))

        // Notify all subscribers
        subscribers.forEach { it.onEvent(event, state) }

        // Notify typed subscribers
        @Suppress("UNCHECKED_CAST")
        typedSubscribers[event::class]?.forEach { subscriber ->
            (subscriber as TypedEventSubscriber<GameEvent>).onEvent(event, state)
        }
    }

    /**
     * Publish multiple game events.
     */
    fun publishAll(events: List<GameEvent>, state: GameState) {
        events.forEach { publish(it, state) }
    }

    /**
     * Publish events from an action result.
     */
    fun publishActionEvents(actionEvents: List<GameActionEvent>, state: GameState) {
        for (actionEvent in actionEvents) {
            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)
            publishAll(gameEvents, state)
        }
    }

    /**
     * Publish events from effect execution.
     */
    fun publishEffectEvents(effectEvents: List<EffectEvent>, state: GameState) {
        for (effectEvent in effectEvents) {
            val gameEvents = GameEventConverter.fromEffectEvent(effectEvent)
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
    fun getRecentEvents(count: Int): List<GameEvent> =
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
interface EventSubscriber {
    fun onEvent(event: GameEvent, state: GameState)
}

/**
 * Typed subscriber for specific event types.
 */
interface TypedEventSubscriber<T : GameEvent> {
    fun onEvent(event: T, state: GameState)
}

/**
 * An event with its timestamp.
 */
data class TimestampedEvent(
    val timestamp: Long,
    val event: GameEvent
)

/**
 * Trigger-aware event subscriber that collects pending triggers.
 *
 * Use this subscriber with an event bus to automatically detect
 * triggered abilities as events occur.
 */
class TriggerCollector(
    private val triggerDetector: TriggerDetector,
    private val abilityRegistry: AbilityRegistry
) : EventSubscriber {

    private val pendingTriggers = mutableListOf<PendingTrigger>()

    override fun onEvent(event: GameEvent, state: GameState) {
        val triggers = triggerDetector.detectTriggers(state, listOf(event), abilityRegistry)
        pendingTriggers.addAll(triggers)
    }

    /**
     * Get and clear all pending triggers.
     */
    fun drainPendingTriggers(): List<PendingTrigger> {
        val result = pendingTriggers.toList()
        pendingTriggers.clear()
        return result
    }

    /**
     * Peek at pending triggers without clearing.
     */
    fun peekPendingTriggers(): List<PendingTrigger> = pendingTriggers.toList()

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
) : EventSubscriber {

    override fun onEvent(event: GameEvent, state: GameState) {
        logFn(formatEvent(event))
    }

    private fun formatEvent(event: GameEvent): String {
        return when (event) {
            is GameEvent.EnteredBattlefield -> "${event.cardName} entered the battlefield"
            is GameEvent.LeftBattlefield -> "${event.cardName} left the battlefield"
            is GameEvent.CreatureDied -> "${event.cardName} died"
            is GameEvent.CardExiled -> "${event.cardName} was exiled"
            is GameEvent.ReturnedToHand -> "${event.cardName} returned to hand"
            is GameEvent.CardDrawn -> "Player drew ${event.cardName}"
            is GameEvent.CardDiscarded -> "Player discarded ${event.cardName}"
            is GameEvent.CombatBegan -> "Combat began"
            is GameEvent.AttackerDeclared -> "${event.cardName} is attacking"
            is GameEvent.BlockerDeclared -> "${event.blockerName} is blocking"
            is GameEvent.CombatEnded -> "Combat ended"
            is GameEvent.DamageDealtToPlayer -> "${event.amount} damage dealt to player"
            is GameEvent.DamageDealtToCreature -> "${event.amount} damage dealt to creature"
            is GameEvent.LifeGained -> "Player gained ${event.amount} life (now ${event.newTotal})"
            is GameEvent.LifeLost -> "Player lost ${event.amount} life (now ${event.newTotal})"
            is GameEvent.UpkeepBegan -> "Upkeep began"
            is GameEvent.EndStepBegan -> "End step began"
            is GameEvent.SpellCast -> "${event.spellName} was cast"
            is GameEvent.PermanentTapped -> "${event.cardName} was tapped"
            is GameEvent.PermanentUntapped -> "${event.cardName} was untapped"
            is GameEvent.CountersAdded -> "${event.count} ${event.counterType} counter(s) added"
            is GameEvent.CountersRemoved -> "${event.count} ${event.counterType} counter(s) removed"
            is GameEvent.PlayerLost -> "Player lost: ${event.reason}"
            is GameEvent.GameEnded -> "Game ended. Winner: ${event.winnerId ?: "None (draw)"}"
        }
    }
}
