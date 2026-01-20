import { useEffect, useRef, useState, useCallback } from 'react'
import { useGameStore } from '../store/gameStore'
import type { ClientEvent } from '../types'
import { getEventAnimationConfig } from './eventAnimations'
import { DamageEffect } from '../components/effects/DamageEffect'
import { DeathEffect } from '../components/effects/DeathEffect'

/**
 * Active visual effect being displayed.
 */
interface ActiveEffect {
  id: string
  event: ClientEvent
  startTime: number
  duration: number
}

/**
 * Event processor that plays events sequentially with visual effects.
 */
export function EventProcessor() {
  const consumeEvent = useGameStore((state) => state.consumeEvent)
  const pendingEvents = useGameStore((state) => state.pendingEvents)

  const [activeEffects, setActiveEffects] = useState<ActiveEffect[]>([])
  const [, setCurrentEvent] = useState<ClientEvent | null>(null)
  const processingRef = useRef(false)
  const effectIdCounter = useRef(0)

  /**
   * Process the next event from the queue.
   */
  const processNextEvent = useCallback(() => {
    if (processingRef.current) return

    const event = consumeEvent()
    if (!event) {
      setCurrentEvent(null)
      return
    }

    processingRef.current = true
    setCurrentEvent(event)

    const config = getEventAnimationConfig(event)

    // Add visual effect
    const effectId = `effect-${effectIdCounter.current++}`
    setActiveEffects((prev) => [
      ...prev,
      {
        id: effectId,
        event,
        startTime: performance.now(),
        duration: config.duration,
      },
    ])

    // Schedule removal of effect and processing of next event
    setTimeout(() => {
      setActiveEffects((prev) => prev.filter((e) => e.id !== effectId))
      processingRef.current = false
      setCurrentEvent(null)

      // Process next event after a small gap
      setTimeout(() => {
        processNextEvent()
      }, 50)
    }, config.duration + config.delay)
  }, [consumeEvent])

  // Start processing when events arrive
  useEffect(() => {
    if (pendingEvents.length > 0 && !processingRef.current) {
      processNextEvent()
    }
  }, [pendingEvents, processNextEvent])

  return (
    <>
      {/* Render active effects */}
      {activeEffects.map((effect) => (
        <EventEffect key={effect.id} effect={effect} />
      ))}
    </>
  )
}

/**
 * Render the appropriate effect for an event.
 */
function EventEffect({ effect }: { effect: ActiveEffect }) {
  const progress = Math.min(
    1,
    (performance.now() - effect.startTime) / effect.duration
  )

  switch (effect.event.type) {
    case 'damageDealt':
      return (
        <DamageEffect
          targetId={effect.event.targetId}
          amount={effect.event.amount}
          isPlayer={effect.event.targetIsPlayer}
          progress={progress}
        />
      )

    case 'creatureDied':
      return (
        <DeathEffect
          creatureId={effect.event.creatureId}
          progress={progress}
        />
      )

    case 'lifeChanged':
      // Life changes are shown in UI, no 3D effect needed
      return null

    case 'spellCast':
      // Spell cast could show a glow effect
      return null

    default:
      // Most events don't need special 3D effects
      return null
  }
}

/**
 * Hook to check if any events are being processed.
 */
export function useIsProcessingEvents(): boolean {
  const pendingEvents = useGameStore((state) => state.pendingEvents)
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    setIsProcessing(pendingEvents.length > 0)
  }, [pendingEvents])

  return isProcessing
}
