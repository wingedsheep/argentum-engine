package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.GiftGivenEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern as SdkGameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent

/**
 * Categories of engine events that triggered abilities respond to.
 *
 * Used to bucket battlefield entities by which engine event types their triggers care about,
 * reducing trigger detection from O(N*M) (all permanents * all events) to O(K*M) where K
 * is typically much smaller than N (only entities with relevant triggers).
 */
enum class TriggerCategory {
    ZONE_CHANGE,
    DRAW,
    CARD_REVEALED,
    ATTACKERS_DECLARED,
    BLOCKERS_DECLARED,
    DAMAGE_RECEIVED,
    SPELL_CAST,
    SPELL_OR_ABILITY,
    CARD_CYCLED,
    TAPPED,
    UNTAPPED,
    PHASES_IN,
    LIFE_GAIN,
    LIFE_LOSS,
    BECOMES_TARGET,
    TURN_FACE_UP,
    STEP,
    LIBRARY_TO_GRAVEYARD,
    ANY_TO_GRAVEYARD,
    CARDS_LEFT_GRAVEYARD,
    SACRIFICE,
    COMBAT_DAMAGE_BATCH,
    LEAVE_WITHOUT_DYING,
    CREATURES_DIED_BATCH,
    PERMANENTS_ENTERED_BATCH,
    COUNTERS_ADDED,
    GIFT_GIVEN,
    TRANSFORM,
    COMMIT_CRIME,
    CHOOSE_TARGETS,
    DISCARD,
    RING_TEMPTED,
    SCRIED,
    SURVEILED,
    YOU_BEND,
    MANIFESTED_DREAD,
    SEARCH_LIBRARY,
    BECAME_SADDLED,
    BECOMES_ATTACHED,
    SAGA_CHAPTER_RESOLVED,
}

/**
 * Pre-indexed battlefield data for efficient trigger detection.
 *
 * Instead of scanning all N battlefield permanents for every event, entities are
 * bucketed by which event types their triggers respond to. Detection queries only
 * the relevant bucket, which typically contains a small fraction of permanents.
 *
 * Also indexes auras by their attachment targets and pre-computes lists of entities
 * with observer-style damage triggers, eliminating full battlefield scans in the
 * specialized detection methods.
 */
class TriggerIndex(
    private val byCategory: Map<TriggerCategory, List<IndexedEntity>>,
    val aurasByTarget: Map<EntityId, List<IndexedEntity>>,
    val grantProviders: List<GrantProviderEntry>,
    val damageToYouObservers: List<IndexedEntity>,
    val subtypeDamageObservers: List<IndexedEntity>,
    val damageObservers: List<IndexedEntity>,
    val creatureDamageDeathTrackers: List<IndexedEntity>,
) {
    /**
     * Get indexed entities that might have triggers matching the given engine event.
     * Deduplicates across categories when an event maps to multiple categories.
     */
    fun getEntitiesForEvent(event: EngineGameEvent): List<IndexedEntity> {
        val categories = engineEventCategories(event)
        if (categories.isEmpty()) return emptyList()
        if (categories.size == 1) return byCategory[categories[0]] ?: emptyList()

        val seen = HashSet<EntityId>()
        return buildList {
            for (cat in categories) {
                for (entry in byCategory[cat].orEmpty()) {
                    if (seen.add(entry.entityId)) add(entry)
                }
            }
        }
    }

    fun getEntitiesForCategory(category: TriggerCategory): List<IndexedEntity> =
        byCategory[category] ?: emptyList()

    data class IndexedEntity(
        val entityId: EntityId,
        val cardComponent: CardComponent,
        val controllerId: EntityId,
        val abilities: List<TriggeredAbility>,
    )

    /**
     * A grant provider paired with the controller and entity id of the source permanent.
     * The controller is needed to evaluate controller predicates like "you control"; the
     * entity id is needed to honor `GroupFilter.excludeSelf` ("Other creatures you control
     * have …"), so the granting permanent does not grant the ability to itself.
     */
    data class GrantProviderEntry(
        val grant: GrantTriggeredAbility,
        val sourceControllerId: EntityId,
        val sourceEntityId: EntityId,
    )

    companion object {
        /**
         * Trigger categories that are consumed *only* by the dedicated batch / observer detectors
         * via [getEntitiesForCategory], never by the main per-event [getEntitiesForEvent] loop or
         * the graveyard/exile per-event passes (which route through [TriggerMatcher.matchesTrigger],
         * and that returns `false` for these batch event shapes). It is therefore safe to index
         * non-battlefield-zone entities (graveyard, exile) into these categories without risking a
         * double-fire — enabling graveyard-active recursion triggers like Killian's Confidence
         * ("Whenever one or more creatures you control deal combat damage to a player, … return this
         * card from your graveyard to your hand").
         */
        val NON_BATTLEFIELD_BATCH_CATEGORIES: Set<TriggerCategory> = setOf(
            TriggerCategory.COMBAT_DAMAGE_BATCH,
            TriggerCategory.LIBRARY_TO_GRAVEYARD,
            TriggerCategory.ANY_TO_GRAVEYARD,
            TriggerCategory.CARDS_LEFT_GRAVEYARD,
            TriggerCategory.SACRIFICE,
            TriggerCategory.LEAVE_WITHOUT_DYING,
            TriggerCategory.CREATURES_DIED_BATCH,
            TriggerCategory.PERMANENTS_ENTERED_BATCH,
        )

        val EMPTY = TriggerIndex(
            byCategory = emptyMap(),
            aurasByTarget = emptyMap(),
            grantProviders = emptyList(),
            damageToYouObservers = emptyList(),
            subtypeDamageObservers = emptyList(),
            damageObservers = emptyList(),
            creatureDamageDeathTrackers = emptyList(),
        )

        /**
         * Map a SDK trigger type to the engine event categories it responds to.
         * Only includes categories for triggers handled in the main detectTriggersForEvent loop.
         */
        fun triggerToCategories(trigger: SdkGameEvent, binding: TriggerBinding): List<TriggerCategory> {
            // ATTACHED triggers are generally handled by AttachmentTriggerDetector via the
            // aurasByTarget index. Exception: BlocksOrBecomesBlockedByEvent needs the full
            // BlockersDeclaredEvent block map to compute the equipped creature's combat partner,
            // so it stays indexed under BLOCKERS_DECLARED and is handled in the main
            // detectTriggersForEvent loop (which resolves ATTACHED → the equipped creature).
            if (binding == TriggerBinding.ATTACHED &&
                trigger !is SdkGameEvent.BlocksOrBecomesBlockedByEvent
            ) return emptyList()

            return when (trigger) {
                is SdkGameEvent.ZoneChangeEvent -> listOf(TriggerCategory.ZONE_CHANGE)
                // "Whenever you create a token" matches token-creation ZoneChangeEvents (fromZone ==
                // null), so it indexes under the same category as those events (TriggerMatcher.
                // matchesTokenCreationTrigger does the create-specific filtering).
                is SdkGameEvent.TokenCreationEvent -> listOf(TriggerCategory.ZONE_CHANGE)
                is SdkGameEvent.DrawEvent -> listOf(TriggerCategory.DRAW)
                is SdkGameEvent.NthCardDrawnEvent -> listOf(TriggerCategory.DRAW)
                is SdkGameEvent.CardRevealedFromDrawEvent -> listOf(TriggerCategory.CARD_REVEALED)
                is SdkGameEvent.AttackEvent -> listOf(TriggerCategory.ATTACKERS_DECLARED)
                is SdkGameEvent.YouAttackEvent -> listOf(TriggerCategory.ATTACKERS_DECLARED)
                is SdkGameEvent.CreaturesAttackYouEvent -> listOf(TriggerCategory.ATTACKERS_DECLARED)
                is SdkGameEvent.CreaturesAttackYourOpponentEvent -> listOf(TriggerCategory.ATTACKERS_DECLARED)
                is SdkGameEvent.BlockEvent -> listOf(TriggerCategory.BLOCKERS_DECLARED)
                is SdkGameEvent.BecomesBlockedEvent -> listOf(TriggerCategory.BLOCKERS_DECLARED)
                is SdkGameEvent.BecomesUnblockedEvent -> listOf(TriggerCategory.BLOCKERS_DECLARED)
                is SdkGameEvent.BlocksOrBecomesBlockedByEvent -> listOf(TriggerCategory.BLOCKERS_DECLARED)
                is SdkGameEvent.DamageReceivedEvent ->
                    if (trigger.source == SourceFilter.Any) listOf(TriggerCategory.DAMAGE_RECEIVED) else emptyList()
                is SdkGameEvent.SpellCastEvent -> listOf(TriggerCategory.SPELL_CAST)
                is SdkGameEvent.NthSpellCastEvent -> listOf(TriggerCategory.SPELL_CAST)
                // "When you cast this spell" fires only via TriggerDetector's self-cast path while
                // the spell is on the stack — never index it against battlefield permanents, or a
                // resolved Sage of the Skies would re-fire on every later spell.
                is SdkGameEvent.CastThisSpellEvent -> emptyList()
                is SdkGameEvent.ExpendEvent -> listOf(TriggerCategory.SPELL_CAST)
                is SdkGameEvent.SpellOrAbilityOnStackEvent -> listOf(TriggerCategory.SPELL_OR_ABILITY)
                is SdkGameEvent.AbilityActivatedEvent -> listOf(TriggerCategory.SPELL_OR_ABILITY)
                is SdkGameEvent.CycleEvent -> listOf(TriggerCategory.CARD_CYCLED)
                is SdkGameEvent.TapEvent -> listOf(TriggerCategory.TAPPED)
                is SdkGameEvent.UntapEvent -> listOf(TriggerCategory.UNTAPPED)
                is SdkGameEvent.PhasesInEvent -> listOf(TriggerCategory.PHASES_IN)
                is SdkGameEvent.LifeGainEvent -> listOf(TriggerCategory.LIFE_GAIN)
                is SdkGameEvent.LifeLossEvent -> listOf(TriggerCategory.LIFE_LOSS)
                is SdkGameEvent.LifeGainOrLossEvent -> listOf(TriggerCategory.LIFE_GAIN, TriggerCategory.LIFE_LOSS)
                is SdkGameEvent.BecomesTargetEvent -> listOf(TriggerCategory.BECOMES_TARGET)
                is SdkGameEvent.TurnFaceUpEvent -> listOf(TriggerCategory.TURN_FACE_UP)
                is SdkGameEvent.CreatureTurnedFaceUpEvent -> listOf(TriggerCategory.TURN_FACE_UP)
                is SdkGameEvent.StepEvent -> listOf(TriggerCategory.STEP)
                is SdkGameEvent.CardsPutIntoGraveyardFromLibraryEvent -> listOf(TriggerCategory.LIBRARY_TO_GRAVEYARD)
                is SdkGameEvent.CardsPutIntoYourGraveyardEvent -> listOf(TriggerCategory.ANY_TO_GRAVEYARD)
                is SdkGameEvent.CardsLeftYourGraveyardEvent -> listOf(TriggerCategory.CARDS_LEFT_GRAVEYARD)
                is SdkGameEvent.PermanentsSacrificedEvent -> listOf(TriggerCategory.SACRIFICE)
                is SdkGameEvent.OneOrMoreDealCombatDamageToPlayerEvent -> listOf(TriggerCategory.COMBAT_DAMAGE_BATCH)
                is SdkGameEvent.OneOrMoreDealCombatDamageToYouEvent -> listOf(TriggerCategory.COMBAT_DAMAGE_BATCH)
                is SdkGameEvent.LeaveBattlefieldWithoutDyingEvent -> listOf(TriggerCategory.LEAVE_WITHOUT_DYING)
                is SdkGameEvent.CreaturesYouControlDiedEvent -> listOf(TriggerCategory.CREATURES_DIED_BATCH)
                is SdkGameEvent.PermanentsEnteredEvent -> listOf(TriggerCategory.PERMANENTS_ENTERED_BATCH)
                is SdkGameEvent.CountersPlacedEvent -> listOf(TriggerCategory.COUNTERS_ADDED)
                is SdkGameEvent.GiftGivenEvent -> listOf(TriggerCategory.GIFT_GIVEN)
                is SdkGameEvent.TransformEvent -> listOf(TriggerCategory.TRANSFORM)
                is SdkGameEvent.CommitCrimeEvent -> listOf(TriggerCategory.COMMIT_CRIME)
                is SdkGameEvent.TargetsChosenEvent -> listOf(TriggerCategory.CHOOSE_TARGETS)
                is SdkGameEvent.DiscardEvent -> listOf(TriggerCategory.DISCARD)
                is SdkGameEvent.RingTemptedEvent -> RING_TEMPTED_LIST
                is SdkGameEvent.ScriedEvent -> SCRIED_LIST
                is SdkGameEvent.SurveiledEvent -> SURVEILED_LIST
                // "Whenever you scry or surveil" indexes under both buckets so either engine event
                // finds it; the matcher confirms the event is a scry or a surveil.
                is SdkGameEvent.ScriedOrSurveiledEvent -> SCRIED_OR_SURVEILED_LIST
                is SdkGameEvent.BendPerformedEvent -> BEND_LIST
                is SdkGameEvent.ManifestedDreadEvent -> MANIFESTED_DREAD_LIST
                is SdkGameEvent.SearchLibraryEvent -> SEARCH_LIBRARY_LIST
                is SdkGameEvent.BecameSaddledEvent -> BECAME_SADDLED_LIST
                is SdkGameEvent.BecomesAttachedEvent -> BECOMES_ATTACHED_LIST
                is SdkGameEvent.SagaChapterResolvedEvent -> SAGA_CHAPTER_RESOLVED_LIST
                // These are handled by specialized detect methods, not the main loop
                else -> emptyList()
            }
        }

        /**
         * Map an engine event to the trigger categories that could match in the main loop.
         */
        fun engineEventCategories(event: EngineGameEvent): List<TriggerCategory> = when (event) {
            is ZoneChangeEvent -> ZONE_CHANGE_LIST
            is CardsDrawnEvent -> DRAW_LIST
            is CardRevealedFromDrawEvent -> CARD_REVEALED_LIST
            is AttackersDeclaredEvent -> ATTACKERS_DECLARED_LIST
            is BlockersDeclaredEvent -> BLOCKERS_DECLARED_LIST
            is DamageDealtEvent -> DAMAGE_RECEIVED_LIST
            is SpellCastEvent -> SPELL_CAST_AND_ABILITY_LIST
            is AbilityActivatedEvent -> SPELL_OR_ABILITY_LIST
            is AbilityTriggeredEvent -> SPELL_OR_ABILITY_LIST
            is CardCycledEvent -> CARD_CYCLED_LIST
            is TappedEvent -> TAPPED_LIST
            is UntappedEvent -> UNTAPPED_LIST
            is com.wingedsheep.engine.core.PhasedInEvent -> PHASES_IN_LIST
            is LifeChangedEvent -> when (event.reason) {
                LifeChangeReason.LIFE_GAIN -> LIFE_GAIN_LIST
                LifeChangeReason.DAMAGE, LifeChangeReason.LIFE_LOSS, LifeChangeReason.PAYMENT -> LIFE_LOSS_LIST
            }
            is BecomesTargetEvent -> BECOMES_TARGET_LIST
            is TurnFaceUpEvent -> TURN_FACE_UP_LIST
            is CountersAddedEvent -> COUNTERS_ADDED_LIST
            is GiftGivenEvent -> GIFT_GIVEN_LIST
            is com.wingedsheep.engine.core.TransformedEvent -> TRANSFORM_LIST
            is com.wingedsheep.engine.core.CommitCrimeEvent -> COMMIT_CRIME_LIST
            is com.wingedsheep.engine.core.TargetsChosenEvent -> CHOOSE_TARGETS_LIST
            is CardsDiscardedEvent -> DISCARD_LIST
            is com.wingedsheep.engine.core.RingTemptedEvent -> RING_TEMPTED_LIST
            is com.wingedsheep.engine.core.ScriedEvent -> SCRIED_LIST
            is com.wingedsheep.engine.core.SurveiledEvent -> SURVEILED_LIST
            is com.wingedsheep.engine.core.BendPerformedEvent -> BEND_LIST
            is com.wingedsheep.engine.core.ManifestedDreadEvent -> MANIFESTED_DREAD_LIST
            is com.wingedsheep.engine.core.LibrarySearchedEvent -> SEARCH_LIBRARY_LIST
            is com.wingedsheep.engine.core.BecameSaddledEvent -> BECAME_SADDLED_LIST
            is com.wingedsheep.engine.core.PermanentAttachedEvent -> BECOMES_ATTACHED_LIST
            is com.wingedsheep.engine.core.SagaChapterResolvedEvent -> SAGA_CHAPTER_RESOLVED_LIST
            else -> emptyList()
        }

        // Pre-allocated lists to avoid allocation on every event
        private val ZONE_CHANGE_LIST = listOf(TriggerCategory.ZONE_CHANGE)
        private val DRAW_LIST = listOf(TriggerCategory.DRAW)
        private val CARD_REVEALED_LIST = listOf(TriggerCategory.CARD_REVEALED)
        private val ATTACKERS_DECLARED_LIST = listOf(TriggerCategory.ATTACKERS_DECLARED)
        private val BLOCKERS_DECLARED_LIST = listOf(TriggerCategory.BLOCKERS_DECLARED)
        private val DAMAGE_RECEIVED_LIST = listOf(TriggerCategory.DAMAGE_RECEIVED)
        private val SPELL_CAST_AND_ABILITY_LIST = listOf(TriggerCategory.SPELL_CAST, TriggerCategory.SPELL_OR_ABILITY)
        private val SPELL_OR_ABILITY_LIST = listOf(TriggerCategory.SPELL_OR_ABILITY)
        private val CARD_CYCLED_LIST = listOf(TriggerCategory.CARD_CYCLED)
        private val TAPPED_LIST = listOf(TriggerCategory.TAPPED)
        private val UNTAPPED_LIST = listOf(TriggerCategory.UNTAPPED)
        private val PHASES_IN_LIST = listOf(TriggerCategory.PHASES_IN)
        private val LIFE_GAIN_LIST = listOf(TriggerCategory.LIFE_GAIN)
        private val LIFE_LOSS_LIST = listOf(TriggerCategory.LIFE_LOSS)
        private val BECOMES_TARGET_LIST = listOf(TriggerCategory.BECOMES_TARGET)
        private val TURN_FACE_UP_LIST = listOf(TriggerCategory.TURN_FACE_UP)
        private val COUNTERS_ADDED_LIST = listOf(TriggerCategory.COUNTERS_ADDED)
        private val GIFT_GIVEN_LIST = listOf(TriggerCategory.GIFT_GIVEN)
        private val TRANSFORM_LIST = listOf(TriggerCategory.TRANSFORM)
        private val COMMIT_CRIME_LIST = listOf(TriggerCategory.COMMIT_CRIME)
        private val CHOOSE_TARGETS_LIST = listOf(TriggerCategory.CHOOSE_TARGETS)
        private val DISCARD_LIST = listOf(TriggerCategory.DISCARD)
        private val RING_TEMPTED_LIST = listOf(TriggerCategory.RING_TEMPTED)
        private val SCRIED_LIST = listOf(TriggerCategory.SCRIED)
        private val SURVEILED_LIST = listOf(TriggerCategory.SURVEILED)
        private val SCRIED_OR_SURVEILED_LIST = listOf(TriggerCategory.SCRIED, TriggerCategory.SURVEILED)
        private val BEND_LIST = listOf(TriggerCategory.YOU_BEND)
        private val MANIFESTED_DREAD_LIST = listOf(TriggerCategory.MANIFESTED_DREAD)
        private val SEARCH_LIBRARY_LIST = listOf(TriggerCategory.SEARCH_LIBRARY)
        private val BECAME_SADDLED_LIST = listOf(TriggerCategory.BECAME_SADDLED)
        private val BECOMES_ATTACHED_LIST = listOf(TriggerCategory.BECOMES_ATTACHED)
        private val SAGA_CHAPTER_RESOLVED_LIST = listOf(TriggerCategory.SAGA_CHAPTER_RESOLVED)
    }
}
