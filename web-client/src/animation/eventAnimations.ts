import type { ClientEvent } from '../types'

/**
 * Animation configuration for an event type.
 */
export interface EventAnimationConfig {
  /** Duration of the animation in milliseconds */
  duration: number
  /** Delay before starting the animation */
  delay: number
  /** Priority for ordering (higher = later in queue) */
  priority: number
  /** Whether this event should be processed immediately without delay */
  immediate: boolean
}

/**
 * Default animation configurations by event type.
 */
export const EVENT_ANIMATION_CONFIGS: Record<ClientEvent['type'], EventAnimationConfig> = {
  // Life/Damage events - quick feedback
  lifeChanged: {
    duration: 500,
    delay: 0,
    priority: 1,
    immediate: false,
  },
  damageDealt: {
    duration: 600,
    delay: 0,
    priority: 2,
    immediate: false,
  },

  // Card movement events - visible transitions
  cardDrawn: {
    duration: 400,
    delay: 100,
    priority: 3,
    immediate: false,
  },
  cardDiscarded: {
    duration: 350,
    delay: 0,
    priority: 3,
    immediate: false,
  },
  permanentEntered: {
    duration: 500,
    delay: 0,
    priority: 4,
    immediate: false,
  },
  permanentLeft: {
    duration: 400,
    delay: 0,
    priority: 4,
    immediate: false,
  },

  // Combat events
  creatureAttacked: {
    duration: 300,
    delay: 0,
    priority: 5,
    immediate: false,
  },
  creatureBlocked: {
    duration: 300,
    delay: 0,
    priority: 5,
    immediate: false,
  },
  creatureDied: {
    duration: 700,
    delay: 0,
    priority: 6,
    immediate: false,
  },

  // Spell/Ability events
  spellCast: {
    duration: 400,
    delay: 0,
    priority: 3,
    immediate: false,
  },
  spellResolved: {
    duration: 300,
    delay: 0,
    priority: 4,
    immediate: false,
  },
  spellCountered: {
    duration: 500,
    delay: 0,
    priority: 4,
    immediate: false,
  },
  abilityTriggered: {
    duration: 300,
    delay: 0,
    priority: 3,
    immediate: false,
  },
  abilityActivated: {
    duration: 300,
    delay: 0,
    priority: 3,
    immediate: false,
  },

  // State change events - quick
  permanentTapped: {
    duration: 200,
    delay: 0,
    priority: 2,
    immediate: true,
  },
  permanentUntapped: {
    duration: 200,
    delay: 0,
    priority: 2,
    immediate: true,
  },
  counterAdded: {
    duration: 400,
    delay: 0,
    priority: 3,
    immediate: false,
  },
  counterRemoved: {
    duration: 400,
    delay: 0,
    priority: 3,
    immediate: false,
  },

  // Mana events - very quick
  manaAdded: {
    duration: 200,
    delay: 0,
    priority: 1,
    immediate: true,
  },

  // Game state events - prominent
  playerLost: {
    duration: 1000,
    delay: 0,
    priority: 10,
    immediate: false,
  },
  gameEnded: {
    duration: 1500,
    delay: 0,
    priority: 10,
    immediate: false,
  },
}

/**
 * Get animation config for an event.
 */
export function getEventAnimationConfig(event: ClientEvent): EventAnimationConfig {
  return EVENT_ANIMATION_CONFIGS[event.type]
}

/**
 * Calculate total duration for processing a list of events.
 */
export function calculateEventQueueDuration(events: ClientEvent[]): number {
  let totalDuration = 0

  for (const event of events) {
    const config = getEventAnimationConfig(event)
    if (!config.immediate) {
      totalDuration += config.duration + config.delay
    }
  }

  return totalDuration
}

/**
 * Sort events by priority for sequential processing.
 */
export function sortEventsByPriority(events: ClientEvent[]): ClientEvent[] {
  return [...events].sort((a, b) => {
    const configA = getEventAnimationConfig(a)
    const configB = getEventAnimationConfig(b)
    return configA.priority - configB.priority
  })
}
