package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
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
import com.wingedsheep.sdk.scripting.GameEvent as SdkGameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityToCreatureGroup
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
    LIFE_GAIN,
    LIFE_LOSS,
    BECOMES_TARGET,
    TURN_FACE_UP,
    STEP,
    LIBRARY_TO_GRAVEYARD,
    SACRIFICE,
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
     * A grant provider paired with the controller of the source permanent.
     * Needed to evaluate controller predicates like "you control" relative to the source.
     */
    data class GrantProviderEntry(
        val grant: GrantTriggeredAbilityToCreatureGroup,
        val sourceControllerId: EntityId,
    )

    companion object {
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
            // ATTACHED triggers are handled by AttachmentTriggerDetector via aurasByTarget index
            if (binding == TriggerBinding.ATTACHED) return emptyList()

            return when (trigger) {
                is SdkGameEvent.ZoneChangeEvent -> listOf(TriggerCategory.ZONE_CHANGE)
                is SdkGameEvent.DrawEvent -> listOf(TriggerCategory.DRAW)
                is SdkGameEvent.CardRevealedFromDrawEvent -> listOf(TriggerCategory.CARD_REVEALED)
                is SdkGameEvent.AttackEvent -> listOf(TriggerCategory.ATTACKERS_DECLARED)
                is SdkGameEvent.YouAttackEvent -> listOf(TriggerCategory.ATTACKERS_DECLARED)
                is SdkGameEvent.BlockEvent -> listOf(TriggerCategory.BLOCKERS_DECLARED)
                is SdkGameEvent.BecomesBlockedEvent -> listOf(TriggerCategory.BLOCKERS_DECLARED)
                is SdkGameEvent.BlocksOrBecomesBlockedByEvent -> listOf(TriggerCategory.BLOCKERS_DECLARED)
                is SdkGameEvent.DamageReceivedEvent ->
                    if (trigger.source == SourceFilter.Any) listOf(TriggerCategory.DAMAGE_RECEIVED) else emptyList()
                is SdkGameEvent.SpellCastEvent -> listOf(TriggerCategory.SPELL_CAST)
                is SdkGameEvent.ExpendEvent -> listOf(TriggerCategory.SPELL_CAST)
                is SdkGameEvent.SpellOrAbilityOnStackEvent -> listOf(TriggerCategory.SPELL_OR_ABILITY)
                is SdkGameEvent.CycleEvent -> listOf(TriggerCategory.CARD_CYCLED)
                is SdkGameEvent.TapEvent -> listOf(TriggerCategory.TAPPED)
                is SdkGameEvent.UntapEvent -> listOf(TriggerCategory.UNTAPPED)
                is SdkGameEvent.LifeGainEvent -> listOf(TriggerCategory.LIFE_GAIN)
                is SdkGameEvent.LifeLossEvent -> listOf(TriggerCategory.LIFE_LOSS)
                is SdkGameEvent.LifeGainOrLossEvent -> listOf(TriggerCategory.LIFE_GAIN, TriggerCategory.LIFE_LOSS)
                is SdkGameEvent.BecomesTargetEvent -> listOf(TriggerCategory.BECOMES_TARGET)
                is SdkGameEvent.TurnFaceUpEvent -> listOf(TriggerCategory.TURN_FACE_UP)
                is SdkGameEvent.CreatureTurnedFaceUpEvent -> listOf(TriggerCategory.TURN_FACE_UP)
                is SdkGameEvent.StepEvent -> listOf(TriggerCategory.STEP)
                is SdkGameEvent.CardsPutIntoGraveyardFromLibraryEvent -> listOf(TriggerCategory.LIBRARY_TO_GRAVEYARD)
                is SdkGameEvent.PermanentsSacrificedEvent -> listOf(TriggerCategory.SACRIFICE)
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
            is LifeChangedEvent -> when (event.reason) {
                LifeChangeReason.LIFE_GAIN -> LIFE_GAIN_LIST
                LifeChangeReason.DAMAGE, LifeChangeReason.LIFE_LOSS, LifeChangeReason.PAYMENT -> LIFE_LOSS_LIST
            }
            is BecomesTargetEvent -> BECOMES_TARGET_LIST
            is TurnFaceUpEvent -> TURN_FACE_UP_LIST
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
        private val LIFE_GAIN_LIST = listOf(TriggerCategory.LIFE_GAIN)
        private val LIFE_LOSS_LIST = listOf(TriggerCategory.LIFE_LOSS)
        private val BECOMES_TARGET_LIST = listOf(TriggerCategory.BECOMES_TARGET)
        private val TURN_FACE_UP_LIST = listOf(TriggerCategory.TURN_FACE_UP)
    }
}
