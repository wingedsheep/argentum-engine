package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import kotlinx.serialization.Serializable

/**
 * Marks a permanent as tapped.
 */
@Serializable
data object TappedComponent : Component

/**
 * Marks a creature as having summoning sickness.
 * Removed at the beginning of the controller's turn.
 */
@Serializable
data object SummoningSicknessComponent : Component

/**
 * Marks a permanent as phased out (Rule 702.26).
 *
 * A phased-out permanent is treated as though it doesn't exist — it's excluded from
 * [com.wingedsheep.engine.state.GameState.getBattlefield], which in turn removes it
 * from state projection, trigger detection, targeting, combat, and state-based actions.
 * It stays physically in the battlefield zone (phasing is not a zone change) and keeps
 * its tapped state, counters, and attachments.
 *
 * It phases back in before [phasedOutByController] untaps during their next untap step
 * (see `BeginningPhaseManager.performUntapStep`). The controller at phase-out time is
 * stored here because, while phased out, the permanent has no projected controller.
 */
@Serializable
data class PhasedOutComponent(
    val phasedOutByController: EntityId,
    /**
     * When non-null, this permanent was phased out "until [phaseInOnSourceLeaves] leaves the
     * battlefield" (Oubliette). It does NOT phase in at its controller's untap step; instead the
     * source's leaves-battlefield trigger ([com.wingedsheep.sdk.scripting.effects.PhaseInLinkedToSourceEffect])
     * phases it back in. Null for ordinary phasing (phases in at the next untap, Rule 702.26a).
     */
    val phaseInOnSourceLeaves: EntityId? = null,
    /** Tap this permanent when it phases back in (Oubliette: "Tap that creature as it phases in"). */
    val tapOnPhaseIn: Boolean = false
) : Component

/**
 * Marks a permanent as having been cast from its controller's hand.
 * Added when a creature spell resolves from the stack after being cast from hand.
 * Used by cards like Phage the Untouchable to check how a creature entered the battlefield.
 */
@Serializable
data object CastFromHandComponent : Component

/**
 * Marks a permanent as having been cast from a graveyard (e.g., flashback, encore,
 * or ongoing permission to cast from graveyard). Added when a spell resolves with
 * castFromZone == GRAVEYARD. Used by triggers that care whether an entering creature
 * was cast from a graveyard (e.g., Twilight Diviner).
 */
@Serializable
data object CastFromGraveyardComponent : Component

/**
 * Marks a permanent that entered the battlefield directly from a graveyard via a
 * reanimation effect (not via casting). Added in the battlefield-entry path when
 * the zone transition's fromZone is GRAVEYARD. Used by triggers that care whether
 * an entering creature was reanimated (e.g., Twilight Diviner).
 */
@Serializable
data object EnteredFromGraveyardComponent : Component

/**
 * Marks a permanent as having been cast for its warp cost.
 * Added when a warped spell resolves from the stack.
 * At the beginning of the next end step, this permanent is exiled.
 * The warp loop continues from exile — the card can be re-cast for its warp cost.
 */
@Serializable
data object WarpedComponent : Component

/**
 * Marks a permanent as having been cast for its evoke cost.
 * Added when an evoked spell resolves from the stack.
 * TriggerDetector detects this on ETB and creates a "sacrifice self" delayed trigger.
 */
@Serializable
data object EvokedComponent : Component

/**
 * Marks a permanent as saddled (CR 702.171b). Added when a Saddle ability resolves.
 * "Saddled" is a marker designation with no inherent rules meaning — Mount payoffs read it
 * via [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsSaddled] to gate "while
 * saddled" triggers and "as long as it's saddled" statics.
 *
 * Transient: stays until end of turn (cleared in [CleanupPhaseManager.cleanupEndOfTurn]) or
 * until the permanent leaves the battlefield (a fresh entity has no battlefield components).
 * It is engine state, not a copiable value, so copy effects don't carry it.
 */
@Serializable
data object SaddledComponent : Component

/**
 * Records the distinct creatures that have crewed (CR 702.122) or saddled (CR 702.171) this
 * permanent during the current turn — the creatures tapped to pay a Crew or Saddle cost on it.
 * A permanent is only ever a Vehicle (crew) or a Mount (saddle), so one set covers both keywords.
 *
 * Two payoff shapes read this, both source-relative (keyed off the ability's source permanent):
 *  - membership, via
 *    [com.wingedsheep.sdk.scripting.predicates.StatePredicate.CrewedOrSaddledSourceThisTurn], for
 *    "target/choose/return a creature that crewed/saddled it this turn" (Giant Beaver, Rambling
 *    Possum, The Gitrog, Calamity) — the target system already restricts those to live creatures.
 *  - count, via [com.wingedsheep.sdk.scripting.values.DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn],
 *    for "for each creature that crewed it this turn" (Luxurious Locomotive). The recorded ids
 *    persist even after a contributor leaves the battlefield, so the count includes creatures no
 *    longer present as the ability resolves (per the Luxurious Locomotive ruling).
 *
 * Transient: the union across every crew/saddle activation this turn (saddle may be activated
 * repeatedly), cleared in [CleanupPhaseManager.cleanupEndOfTurn] and naturally gone if this
 * permanent itself leaves the battlefield (a fresh entity has no battlefield components).
 */
@Serializable
data class CrewSaddleContributorsComponent(
    val creatureIds: Set<EntityId>,
    /**
     * How many times the *crew* ability has been activated on this permanent this turn. Used to
     * enforce a "Crew N. Activate only once each turn." cap (Luxurious Locomotive). Saddle
     * activations don't increment it. Reset with the rest of the component at end of turn.
     */
    val crewActivations: Int = 0
) : Component

/**
 * Marks a permanent as having been cast for its impending cost (CR 702.176a).
 * The "isn't a creature" static and the end-step time-counter trigger both gate on
 * impending-cost-paid AND has-time-counter, so this marker survives for as long as
 * the permanent stays on the battlefield even after the last time counter is gone.
 */
@Serializable
data object CastForImpendingComponent : Component

/**
 * Marks a creature permanent as prepared (Secrets of Strixhaven, [com.wingedsheep.sdk.model.CardLayout.PREPARE]).
 *
 * A creature with a prepare spell becomes prepared as it enters (per the "This creature enters
 * prepared" keyword). When it becomes prepared, its controller creates a copy of the card's
 * prepare spell ([com.wingedsheep.sdk.model.CardDefinition.cardFaces] index 0) in exile and may
 * cast that copy (paying its cost). [exileCopyId] links to that exiled copy so it can be removed
 * if the permanent stops being prepared or leaves the battlefield, and casting the copy removes
 * this component (the creature stops being prepared).
 *
 * Transient engine state, not a copiable value (a copy of a prepared creature is not prepared).
 * Naturally gone when the permanent leaves the battlefield (a fresh entity has no battlefield
 * components); the linked exile copy is cleaned up by [com.wingedsheep.engine.core.StateBasedActionChecker]-adjacent
 * cleanup when the source leaves.
 */
@Serializable
data class PreparedComponent(
    val exileCopyId: EntityId
) : Component

/**
 * Marks an exiled card as the prepare-spell copy of a prepared permanent (Secrets of Strixhaven).
 *
 * [sourceId] is the prepared permanent on the battlefield. This copy is cast as the card's prepare
 * spell — face index 0 of [com.wingedsheep.sdk.model.CardDefinition.cardFaces] — so the cast-from-exile
 * enumerator emits the cast with `faceIndex = 0` and the prepare spell's cost/targets. Per the
 * rulings, this copy persists in exile (it is not removed by the "copies in non-stack zones cease to
 * exist" state-based action) for as long as the source is on the battlefield and prepared.
 */
@Serializable
data class PreparedSpellCopyComponent(
    val sourceId: EntityId
) : Component

/**
 * Marks an exiled card as suspended (CR 702.62). The marker — not the card's printed
 * abilities — is what the engine keys on, so an arbitrary card with no printed suspend can
 * be suspended (e.g. Taigam, Master Opportunist exiles the spell you cast and grants it
 * suspend). While present on an exiled card, [com.wingedsheep.engine.event.TriggerAbilityResolver]
 * grants it [com.wingedsheep.sdk.scripting.Suspend.countdownAbility] — the owner's-upkeep
 * countdown that removes a time counter and plays the card for free when the last is gone.
 *
 * Stripped when the card leaves exile by a non-cast path and when it leaves the battlefield.
 */
@Serializable
data object SuspendedComponent : Component

/**
 * Marks an exiled card as a Paradigm spell (Secrets of Strixhaven). Attached as a Paradigm spell
 * lands in exile on its own resolution. While present on an exiled card,
 * [com.wingedsheep.engine.event.TriggerAbilityResolver] grants it
 * [com.wingedsheep.sdk.scripting.Paradigm.recastAbility] — the precombat-main trigger that lets the
 * owner cast a free *copy* of the card each turn. The marker's presence is the gate: a Lesson exiled
 * by some other effect (graveyard hate, opponent removal) carries no marker and so never recurs.
 *
 * Stripped when the card leaves exile by a non-cast path and when it leaves the battlefield.
 */
@Serializable
data object ParadigmComponent : Component

/**
 * Records the mana colors spent to cast this permanent.
 * Used by mana-spent-gated trigger conditions (e.g., "if {W}{W} was spent to cast it").
 * Stripped when the permanent leaves the battlefield.
 */
@Serializable
data class CastRecordComponent(
    val whiteSpent: Int = 0,
    val blueSpent: Int = 0,
    val blackSpent: Int = 0,
    val redSpent: Int = 0,
    val greenSpent: Int = 0,
    val colorlessSpent: Int = 0
) : Component

/**
 * Marks a permanent so that if it would leave the battlefield, it is exiled instead.
 * Used by Kheru Lich Lord, Whip of Erebos, Sneak Attack, and similar reanimation effects.
 */
@Serializable
data object ExileOnLeaveBattlefieldComponent : Component

/**
 * Records that this permanent entered the battlefield via a tracked ability of a
 * specific source permanent. Used to satisfy "if it wasn't put onto the battlefield
 * with this ability" anti-loop clauses (e.g., Kodama of the East Tree).
 *
 * Set by `MoveCollectionEffect` when the executor is configured to tag the move with
 * the resolving ability's source. Read by [com.wingedsheep.sdk.scripting.conditions.Condition.TriggeringEntityWasNotPutByThisSource]
 * at trigger-evaluation time. The component is cleared on every subsequent move to the
 * battlefield (so a permanent that left and re-entered via a different path starts
 * clean).
 */
@Serializable
data class EnteredViaAbilityComponent(val sourceId: EntityId) : Component

/**
 * Tracks the current level of a Class enchantment (Rule 716).
 * Added when a Class enters the battlefield (starts at level 1).
 * Level advances when the player pays the level-up cost.
 */
@Serializable
data class ClassLevelComponent(
    val currentLevel: Int = 1
) : Component {
    fun withLevelUp(): ClassLevelComponent = copy(currentLevel = currentLevel + 1)
}

/**
 * Tracks Saga state: which chapters have already triggered.
 * Added when a Saga enters the battlefield.
 */
@Serializable
data class SagaComponent(
    val triggeredChapters: Set<Int> = emptySet()
) : Component {
    fun withChapterTriggered(chapter: Int): SagaComponent =
        copy(triggeredChapters = triggeredChapters + chapter)
}

/**
 * Tracks creature types "noted" by a permanent — the set of types this permanent's effects
 * have recorded so far. Generic enough to serve any "note a creature type for this <permanent>"
 * card: Long List of the Ents (LTR Saga that notes a different type each chapter), conspiracy-
 * like remembered-type cards, and any future "remembered name / type" tracking.
 *
 * Lives on the source permanent's container, so multiple noting permanents on the same battlefield
 * keep independent sets. The component is implicitly cleared when the permanent leaves play: per
 * CR 400.7, a permanent that moves zones becomes a new object with no memory of its previous
 * existence, so the component vanishes with the old entity and no LTB handler is needed.
 *
 * Created on demand by the [com.wingedsheep.engine.handlers.continuations] resumer for
 * `NoteCreatureTypeEffect`; consumers read it via `state.getEntity(sourceId)?.get<NotedCreatureTypesComponent>()`
 * to enumerate which types are already noted (for dedup in the choice UI).
 */
@Serializable
data class NotedCreatureTypesComponent(
    val types: Set<String> = emptySet()
) : Component {
    fun withAdded(type: String): NotedCreatureTypesComponent =
        copy(types = types + type)
}

/**
 * Counters on a permanent.
 */
@Serializable
data class CountersComponent(
    val counters: Map<CounterType, Int> = emptyMap()
) : Component {
    fun getCount(type: CounterType): Int = counters[type] ?: 0

    fun withAdded(type: CounterType, amount: Int): CountersComponent {
        val current = getCount(type)
        // Saturating add: doubling a near-billion-counter permanent must clamp, never wrap to a
        // negative `Int` (which would corrupt state silently). See GameLimits.
        return CountersComponent(counters + (type to com.wingedsheep.engine.core.GameLimits.addClamped(current, amount)))
    }

    fun withRemoved(type: CounterType, amount: Int): CountersComponent {
        val current = getCount(type)
        val newCount = (current - amount).coerceAtLeast(0)
        return if (newCount == 0) {
            CountersComponent(counters - type)
        } else {
            CountersComponent(counters + (type to newCount))
        }
    }

    /**
     * Set counters to a specific value.
     * Used for planeswalkers entering the battlefield with starting loyalty.
     */
    fun withCounters(type: CounterType, amount: Int): CountersComponent {
        return if (amount <= 0) {
            CountersComponent(counters - type)
        } else {
            CountersComponent(counters + (type to amount))
        }
    }
}

/**
 * Damage marked on a creature (cleared at cleanup).
 */
@Serializable
data class DamageComponent(
    val amount: Int,
    val deathtouchDamageReceived: Boolean = false
) : Component

/**
 * Aura/Equipment attachment.
 */
@Serializable
data class AttachedToComponent(
    val targetId: EntityId
) : Component

/**
 * Tracks what is attached to this permanent.
 */
@Serializable
data class AttachmentsComponent(
    val attachedIds: List<EntityId>
) : Component

/**
 * Permanent entered the battlefield this turn.
 */
@Serializable
data object EnteredThisTurnComponent : Component

/**
 * Stores replacement effects on a permanent (e.g., Daunting Defender's damage prevention).
 * These are static replacement effects that are continuously active while the permanent
 * is on the battlefield, as opposed to one-shot floating effect shields.
 */
@Serializable
data class ReplacementEffectSourceComponent(
    val replacementEffects: List<ReplacementEffect>
) : Component

/**
 * Timestamp for ordering effects (Rule 613).
 */
@Serializable
data class TimestampComponent(
    val timestamp: Long
) : Component

/**
 * Tracks which activated abilities have been activated this turn.
 * Used for "Activate only once each turn" restrictions and planeswalker loyalty abilities (Rule 606.3).
 * Cleared at end of turn by TurnManager.
 */
@Serializable
data class AbilityActivatedThisTurnComponent(
    val abilityIds: Set<AbilityId> = emptySet(),
    val loyaltyActivationCount: Int = 0,
    /**
     * How many times each ability was activated this turn. Used by
     * [com.wingedsheep.sdk.scripting.ActivationRestriction.MaxPerTurn] (e.g. Phyrexian
     * Battleflies' "Activate no more than twice each turn"). Distinct from [abilityIds],
     * which only records whether an ability was activated at all (once-per-turn).
     */
    val activationCounts: Map<AbilityId, Int> = emptyMap()
) : Component {
    fun withActivated(abilityId: AbilityId): AbilityActivatedThisTurnComponent =
        copy(
            abilityIds = abilityIds + abilityId,
            activationCounts = activationCounts + (abilityId to (activationCounts[abilityId] ?: 0) + 1)
        )

    fun hasActivated(abilityId: AbilityId): Boolean = abilityId in abilityIds

    /** Number of times [abilityId] has been activated this turn. */
    fun activationCount(abilityId: AbilityId): Int = activationCounts[abilityId] ?: 0

    fun withLoyaltyActivated(): AbilityActivatedThisTurnComponent =
        copy(loyaltyActivationCount = loyaltyActivationCount + 1)

    /** @return true if the loyalty activation limit has been reached for the given max. */
    fun hasReachedLoyaltyLimit(maxActivations: Int): Boolean =
        loyaltyActivationCount >= maxActivations
}

/**
 * Tracks which activated abilities have been activated ever (not cleared at end of turn).
 * Used for "Activate only once" restrictions (e.g., Thought Shucker).
 * NOT cleared at end of turn — persists for the permanent's lifetime.
 */
@Serializable
data class AbilityActivatedEverComponent(
    val abilityIds: Set<AbilityId> = emptySet()
) : Component {
    fun withActivated(abilityId: AbilityId): AbilityActivatedEverComponent =
        copy(abilityIds = abilityIds + abilityId)

    fun hasActivated(abilityId: AbilityId): Boolean = abilityId in abilityIds
}

/**
 * Tracks which modes of a modal effect this permanent has already chosen, for
 * "choose one that hasn't been chosen" effects (e.g., Gandalf the Grey). Keyed by
 * the original mode index in the source's [com.wingedsheep.sdk.scripting.effects.ModalEffect].
 *
 * NOT cleared at end of turn — the memory persists for as long as the permanent
 * remains the same object on the battlefield (it resets when the permanent leaves
 * and returns as a new object, per CR 700.4 / object identity).
 */
@Serializable
data class ChosenModesEverComponent(
    val modeIndices: Set<Int> = emptySet()
) : Component {
    fun withChosen(modeIndex: Int): ChosenModesEverComponent =
        copy(modeIndices = modeIndices + modeIndex)

    fun hasChosen(modeIndex: Int): Boolean = modeIndex in modeIndices
}

/**
 * Tracks which triggered abilities have fired this turn for "once each turn" restrictions.
 * Used for cards like Scavenger's Talent: "This ability triggers only once each turn."
 * Cleared at end of turn by CleanupPhaseManager.
 */
@Serializable
data class TriggeredAbilityFiredThisTurnComponent(
    val abilityIds: Set<AbilityId> = emptySet()
) : Component {
    fun withFired(abilityId: AbilityId): TriggeredAbilityFiredThisTurnComponent =
        copy(abilityIds = abilityIds + abilityId)

    fun hasFired(abilityId: AbilityId): Boolean = abilityId in abilityIds
}

/**
 * Tracks which triggered abilities have fired over this permanent's lifetime on the battlefield,
 * for "This ability triggers only once" restrictions (e.g. Acrobatic Cheerleader).
 * Unlike [TriggeredAbilityFiredThisTurnComponent] this is NOT cleared at end of turn — it persists
 * for as long as the permanent stays on the battlefield. It lives on the entity, so it is dropped
 * automatically when the permanent leaves (the permanent re-enters as a new game object, which
 * triggers afresh).
 */
@Serializable
data class TriggeredAbilityFiredEverComponent(
    val abilityIds: Set<AbilityId> = emptySet()
) : Component {
    fun withFired(abilityId: AbilityId): TriggeredAbilityFiredEverComponent =
        copy(abilityIds = abilityIds + abilityId)

    fun hasFired(abilityId: AbilityId): Boolean = abilityId in abilityIds
}

/**
 * Per-permanent latch state for [com.wingedsheep.sdk.scripting.StateTriggeredAbility]
 * instances (CR 603.8). An [AbilityId] is in [latched] iff the engine has fired this
 * state trigger and the condition has not yet become false again — preventing repeat
 * firings while the condition stays true. (See [com.wingedsheep.sdk.scripting.StateTriggeredAbility]
 * for why this latch resets on condition-false rather than on leaves-the-stack.)
 * The [com.wingedsheep.engine.event.StateTriggerPoller]
 * adds the id when emitting a [com.wingedsheep.engine.event.PendingTrigger] and removes
 * it as soon as the condition next evaluates false.
 *
 * NOT cleared at end of turn — the latch follows the permanent for as long as the
 * condition stays true, possibly across turns. Removed automatically when the entity
 * leaves the battlefield (component lives on the entity).
 */
@Serializable
data class StateTriggerLatchesComponent(
    val latched: Set<AbilityId> = emptySet()
) : Component {
    fun withLatched(abilityId: AbilityId): StateTriggerLatchesComponent =
        copy(latched = latched + abilityId)

    fun withoutLatched(abilityId: AbilityId): StateTriggerLatchesComponent =
        copy(latched = latched - abilityId)

    fun isLatched(abilityId: AbilityId): Boolean = abilityId in latched
}

/**
 * Tracks how many times abilities on this permanent have resolved this turn.
 * Used for cards like Harvestrite Host: "if this is the second time this ability has resolved this turn."
 * Cleared at end of turn by CleanupPhaseManager.
 */
@Serializable
data class AbilityResolutionCountThisTurnComponent(
    val count: Int = 0
) : Component {
    fun incremented(): AbilityResolutionCountThisTurnComponent = copy(count = count + 1)
}

/**
 * Marks an attachment source that has already offered its token creation replacement
 * this turn. Used by ReplaceTokenCreationWithAttachedCopy (Mirrormind Crown,
 * Moonlit Meditation) to enforce "first time each turn".
 * Cleared at end of turn by CleanupPhaseManager.
 */
@Serializable
data object TokenReplacementOfferedThisTurnComponent : Component

/**
 * Tracks which controllers have targeted this permanent with spells or abilities this turn.
 * Used for Valiant triggers: "for the first time each turn".
 * Cleared at end of turn by CleanupPhaseManager.
 */
@Serializable
data class TargetedByControllerThisTurnComponent(
    val controllerIds: Set<EntityId> = emptySet()
) : Component {
    fun withController(controllerId: EntityId): TargetedByControllerThisTurnComponent =
        copy(controllerIds = controllerIds + controllerId)

    fun hasBeenTargetedBy(controllerId: EntityId): Boolean = controllerId in controllerIds
}

/**
 * Marks a permanent that has had one or more counters put on it already this turn.
 * Used for "the first time counters have been put on that creature this turn" intervening-if
 * triggers (e.g. Stalwart Successor). Set the first time any counter lands on the permanent
 * (see DamageUtils.markCounterPlacedOnCreature), read to compute the per-event "first this turn"
 * flag, and cleared at end of turn by CleanupPhaseManager.
 */
@Serializable
data object ReceivedCountersThisTurnComponent : Component

/**
 * Tracks which creatures this entity dealt damage to this turn.
 * Used for triggers like Soul Collector: "Whenever a creature dealt damage by Soul Collector this turn dies..."
 * Cleared at end of turn by TurnManager.
 */
@Serializable
data class DamageDealtToCreaturesThisTurnComponent(
    val creatureIds: Set<EntityId> = emptySet()
) : Component {
    fun withCreature(creatureId: EntityId): DamageDealtToCreaturesThisTurnComponent =
        copy(creatureIds = creatureIds + creatureId)
}

/**
 * Last-known snapshot of a source that dealt damage to the bearer this turn — captured at the
 * moment the damage was dealt (CR 608.2h / 603.10a). Holds just what observer-style death triggers
 * need to evaluate a source filter ("dealt damage by a Spider you controlled"): the source's
 * controller and creature-subtypes as they were when it dealt the damage. Stored on the *damaged*
 * creature so it survives a source that died in the same combat.
 */
@Serializable
data class DamageSourceLki(
    val sourceControllerId: EntityId,
    val sourceSubtypes: Set<com.wingedsheep.sdk.core.Subtype> = emptySet(),
    val sourceWasCreature: Boolean = true,
)

/**
 * Tracks the last-known snapshots of all sources that dealt damage to this creature this turn.
 * Read by observer death triggers of the form "whenever another creature dealt damage this turn by
 * [a source matching a filter] dies" (Shelob, Child of Ungoliant). Captured as last-known info on
 * the [com.wingedsheep.engine.core.ZoneChangeEvent] when the bearer leaves the battlefield, and
 * cleared at end of turn by [com.wingedsheep.engine.core.CleanupPhaseManager].
 */
@Serializable
data class DamagedBySourcesThisTurnComponent(
    val sources: Set<DamageSourceLki> = emptySet()
) : Component {
    fun adding(source: DamageSourceLki): DamagedBySourcesThisTurnComponent =
        copy(sources = sources + source)
}

/**
 * Tracks damage dealt to this entity this turn, summed per source-controller player.
 * Used by Grothama-style LTB triggers: "each player draws cards equal to the damage
 * dealt to ~ this turn by sources they controlled." Captured as last-known info on
 * the [com.wingedsheep.engine.core.ZoneChangeEvent] when the bearer leaves the
 * battlefield. Cleared at end of turn by [com.wingedsheep.engine.core.CleanupPhaseManager].
 */
@Serializable
data class DamageDealtByPlayersThisTurnComponent(
    val perPlayer: Map<EntityId, Int> = emptyMap()
) : Component {
    fun adding(playerId: EntityId, amount: Int): DamageDealtByPlayersThisTurnComponent {
        if (amount <= 0) return this
        val current = perPlayer[playerId] ?: 0
        return copy(perPlayer = perPlayer + (playerId to (current + amount)))
    }
}

/**
 * Tracks which permanent types have been used for graveyard casting/playing this turn
 * from a permanent with MayPlayPermanentsFromGraveyard static ability (e.g., Muldrotha).
 * Stored on the Muldrotha entity itself so a new Muldrotha has a fresh set of permissions.
 * Cleared at end of turn by TurnManager.
 *
 * @property usedTypes Set of CardType names that have been used (e.g., "CREATURE", "ARTIFACT", "LAND")
 */
@Serializable
data class GraveyardPlayPermissionUsedComponent(
    val usedTypes: Set<String> = emptySet()
) : Component {
    fun withUsedType(typeName: String): GraveyardPlayPermissionUsedComponent =
        copy(usedTypes = usedTypes + typeName)

    fun hasUsedType(typeName: String): Boolean = typeName in usedTypes
}

/**
 * Marks a permanent as granting shroud to its controller.
 * Used for True Believer: "You have shroud."
 * When the permanent leaves the battlefield, the component goes with it — no cleanup needed.
 */
@Serializable
data object GrantsControllerShroudComponent : Component

/**
 * Marks a permanent as granting hexproof to its controller.
 * Used for Shalai, Voice of Plenty: "You ... have hexproof."
 * Unlike shroud, the controller can still target themselves.
 * When the permanent leaves the battlefield, the component goes with it — no cleanup needed.
 */
@Serializable
data object GrantsControllerHexproofComponent : Component

/**
 * Marks a permanent as granting "can't lose the game" to its controller.
 * Used for Lich's Mastery: "You can't lose the game."
 * When the permanent leaves the battlefield, the component goes with it — no cleanup needed.
 */
@Serializable
data object GrantsCantLoseGameComponent : Component

/**
 * Marks a permanent as granting the Station-using-toughness effect to creatures its
 * controller controls. When a creature with this component's controller taps for a
 * Station ability and its toughness > power, it contributes toughness instead of power.
 *
 * Created by [StaticAbilityHandler] from [StationUsingToughness] static abilities.
 */
@Serializable
data object GrantsStationUsingToughnessComponent : Component

/**
 * Marks a permanent as unable to be targeted by abilities opponents control.
 * Unlike hexproof, spells can still target this permanent.
 * Used for Shanna, Sisay's Legacy: "Shanna can't be the target of abilities your opponents control."
 */
@Serializable
data object CantBeTargetedByOpponentAbilitiesComponent : Component

/**
 * Marks a permanent as granting "can't be blocked" to creatures its controller
 * controls with power or toughness at most [maxValue].
 * Used for Tetsuko Umezawa, Fugitive.
 */
@Serializable
data class GrantCantBeBlockedToSmallCreaturesComponent(val maxValue: Int) : Component

/**
 * Marks a permanent as suppressing hexproof for creatures matching any of [filters].
 *
 * Stores one filter per [SuppressHexproofForGroup] static ability on the permanent so
 * multiple such abilities stack correctly. Each filter is evaluated with the permanent's
 * controller as the "you" context.
 *
 * Created by [StaticAbilityHandler] from [SuppressHexproofForGroup] static abilities.
 */
@Serializable
data class SuppressesHexproofForGroupComponent(
    val filters: List<GroupFilter>
) : Component

/**
 * Marks a permanent as suppressing ward triggers for creatures matching any of [filters].
 *
 * Stores one filter per [SuppressWardForGroup] static ability on the permanent so
 * multiple such abilities stack correctly. Each filter is evaluated with the permanent's
 * controller as the "you" context.
 *
 * Created by [StaticAbilityHandler] from [SuppressWardForGroup] static abilities.
 */
@Serializable
data class SuppressesWardForGroupComponent(
    val filters: List<GroupFilter>
) : Component

/**
 * Tracks entity IDs of cards exiled by this permanent, so they can be
 * returned when the permanent leaves the battlefield.
 *
 * Used for Day of the Dragons-style effects where exiled cards are linked
 * to a specific source permanent.
 *
 * Not stripped by [stripBattlefieldComponents] — intentionally persists when
 * the permanent moves zones so LTB triggers can still read it.
 */
@Serializable
data class LinkedExileComponent(
    val exiledIds: List<EntityId> = emptyList()
) : Component

/**
 * Cards exiled to pay the Craft activation cost that put this permanent onto the battlefield
 * (CR 702.167c). Attached fresh by
 * [com.wingedsheep.engine.handlers.effects.permanent.types.ReturnSelfFromExileTransformedExecutor]
 * when the Craft ability resolves; read by the `DynamicAmount.CraftedMaterialsTotalPower`
 * evaluator on the back face.
 *
 * Stripped on battlefield exit by `ZoneMovementUtils.stripBattlefieldComponents` and on
 * battlefield entry by `ZoneTransitionService.applyBattlefieldEntry` — Rule 400.7 makes any
 * re-entering object a new object with no memory of its prior existence, so a Mastercraft
 * Raptor that subsequently leaves and returns by some other means (blink, reanimate) starts
 * fresh with no crafted materials. The craft-return effect explicitly re-attaches the
 * component on its specific entry path, after the entry strip has run.
 */
@Serializable
data class CraftedFromExiledComponent(
    val exiledIds: List<EntityId> = emptyList()
) : Component

/**
 * Marks a permanent as having been dealt damage this turn.
 * Cleared at end of turn by CleanupPhaseManager.
 * Used for StatePredicate.WasDealtDamageThisTurn.
 */
@Serializable
data object WasDealtDamageThisTurnComponent : Component

/**
 * Marks a permanent as having dealt damage since entering the battlefield.
 * NOT cleared at end of turn — persists for the permanent's lifetime on the battlefield.
 * Used for StatePredicate.HasDealtDamage and SourceHasDealtDamage condition.
 */
@Serializable
data object HasDealtDamageComponent : Component

/**
 * Marks a permanent as having dealt combat damage to a player since entering the battlefield.
 * NOT cleared at end of turn — persists for the permanent's lifetime on the battlefield.
 * Used for StatePredicate.HasDealtCombatDamageToPlayer and SourceHasDealtCombatDamageToPlayer condition.
 */
@Serializable
data object HasDealtCombatDamageToPlayerComponent : Component

/**
 * Records which players this creature dealt combat damage to *this turn*.
 * Cleared at end of turn by CleanupPhaseManager. Used by
 * StatePredicate.DealtCombatDamageToSourceControllerThisTurn — i.e. "a creature that
 * dealt combat damage to you this turn" edicts (Witch-king of Angmar).
 */
@Serializable
data class DealtCombatDamageToPlayersThisTurnComponent(
    val playerIds: Set<EntityId> = emptySet()
) : Component

/**
 * Tracks the turn number when a card was put into the graveyard.
 * Used by effects like Garna, the Bloodflame to return creature cards
 * that were put into the graveyard this turn.
 * Set automatically by GameState.addToZone when zone is GRAVEYARD.
 */
@Serializable
data class GraveyardEntryTurnComponent(
    val turnNumber: Int
) : Component

/**
 * Tracks the turn number when a card was put into exile.
 * Used by effects like Maralen, Fae Ascendant whose static ability is gated to
 * "cards exiled with this permanent this turn". Set automatically by
 * GameState.addToZone when zone is EXILE.
 */
@Serializable
data class ExileEntryTurnComponent(
    val turnNumber: Int
) : Component

/**
 * Marks a permanent as having had its [com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile]
 * permission used this turn. Used to enforce the `oncePerTurn` flag on that static ability
 * (e.g., Maralen, Fae Ascendant). Cleared at end of turn by CleanupPhaseManager.
 */
@Serializable
data object MayCastFromLinkedExileUsedThisTurnComponent : Component

/**
 * Marks a permanent as having had its
 * [com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost] permission used this turn. Used to
 * enforce the `oncePerTurn` flag on that static ability (e.g., Zaffai and the Tempests). Cleared
 * at end of turn by CleanupPhaseManager.
 */
@Serializable
data object MayCastWithoutPayingCostUsedThisTurnComponent : Component
