package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ReplacementEffect
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
 * Marks a permanent as having been kicked when cast.
 * Added when a kicked spell resolves from the stack.
 * Used by cards like Skizzik to check if the kicker cost was paid.
 */
@Serializable
data object WasKickedComponent : Component

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
 * Counters on a permanent.
 */
@Serializable
data class CountersComponent(
    val counters: Map<CounterType, Int> = emptyMap()
) : Component {
    fun getCount(type: CounterType): Int = counters[type] ?: 0

    fun withAdded(type: CounterType, amount: Int): CountersComponent {
        val current = getCount(type)
        return CountersComponent(counters + (type to current + amount))
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
    val loyaltyActivationCount: Int = 0
) : Component {
    fun withActivated(abilityId: AbilityId): AbilityActivatedThisTurnComponent =
        copy(abilityIds = abilityIds + abilityId)

    fun hasActivated(abilityId: AbilityId): Boolean = abilityId in abilityIds

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
 * Marks an equipment that has already offered its token creation replacement this turn.
 * Used by ReplaceTokenCreationWithEquippedCopy (Mirrormind Crown) to enforce "first time each turn".
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
