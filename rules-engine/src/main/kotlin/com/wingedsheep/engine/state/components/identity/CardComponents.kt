package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * Core identity of a card - links to its definition.
 */
@Serializable
data class CardComponent(
    val cardDefinitionId: String,
    val name: String,
    val manaCost: ManaCost,
    val typeLine: TypeLine,
    val oracleText: String = "",
    val baseStats: CreatureStats? = null,
    val baseKeywords: Set<Keyword> = emptySet(),
    val baseFlags: Set<AbilityFlag> = emptySet(),
    val colors: Set<Color> = emptySet(),
    val ownerId: EntityId? = null,
    val spellEffect: Effect? = null,
    val imageUri: String? = null,
    val backFaceImageUri: String? = null,
    /**
     * Precomputed from the card definition: does this card have at least one intrinsic
     * activated ability that isn't a mana ability (and isn't a loyalty ability)? Used by
     * static filters such as Tsabo's Web ("each land with an activated ability that isn't
     * a mana ability"). This reflects the card's printed abilities only — abilities granted
     * by other continuous effects are not counted here.
     */
    val hasNonManaActivatedAbility: Boolean = false,
    /**
     * Precomputed from the card definition: does this card have at least one intrinsic activated
     * ability of any kind (mana, loyalty, or otherwise) activatable from the battlefield? Used by
     * `CardPredicate.HasActivatedAbility` for the craft material clause on The Enigma Jewel ("four
     * or more nonlands with activated abilities"). Unlike [hasNonManaActivatedAbility] this counts
     * mana abilities. Printed abilities only — granted abilities are not counted.
     */
    val hasActivatedAbility: Boolean = false,
    /**
     * The set this card was *originally printed* in (the canonical [CardDefinition.setCode], not the
     * specific printing the player owns). Read by `CardPredicate.OriginallyPrintedInSet` to model
     * "permanent with a name originally printed in [set]" (Golgothian Sylex, ARN City in a Bottle).
     * Null for tokens and any card whose definition carries no set code.
     */
    val originalSetCode: String? = null,
    /**
     * Precomputed from the card definition: does this card have an Adventure
     * ([com.wingedsheep.sdk.model.CardLayout.ADVENTURE])? A static, copyable-independent
     * characteristic of the whole card in any zone — read by `CardPredicate.HasAdventure`
     * (Frantic Firebolt tallies adventurer cards in the graveyard). False for tokens.
     */
    val hasAdventure: Boolean = false,
) : Component {
    // Convenience accessors
    val isCreature: Boolean get() = typeLine.isCreature
    val isLand: Boolean get() = typeLine.isLand
    val isPermanent: Boolean get() = typeLine.isPermanent
    val isAura: Boolean get() = typeLine.isAura
    val isPlaneswalker: Boolean get() = CardType.PLANESWALKER in typeLine.cardTypes
    val manaValue: Int get() = manaCost.cmc
}

/**
 * Marks an entity as a token (not a real card).
 */
@Serializable
data object TokenComponent : Component

/**
 * Provenance record stamped on a token created by a [CreateTokenEffect] with `stampCreator = true`:
 * the entity id of the permanent that created it. Lets later abilities recognize "tokens created
 * with this [permanent]" (CR 111 — a token's creator), read by the
 * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.CreatedBySource] filter against the
 * effect's source. Tetravus uses it to reabsorb only the Tetravite tokens it minted.
 *
 * @property creatorId The entity id of the permanent that created this token.
 */
@Serializable
data class CreatedByComponent(val creatorId: com.wingedsheep.sdk.model.EntityId) : Component

/**
 * Tracks that an entity is a copy of another card.
 * The originalCardDefinitionId preserves what the card originally was (e.g., Clone),
 * while copiedCardDefinitionId tracks what it's currently copying.
 *
 * [originalCardComponent] is the pre-copy [CardComponent] snapshot. Permanent-level
 * copy effects (Clone, Mockingbird, "X becomes a copy of Y") populate it so the
 * card can revert to its printed identity when it leaves the battlefield (CR 400.7
 * — moving zones makes a new object with no memory of the prior copy). Stack-only
 * copies (Storm and friends) leave it null.
 */
@Serializable
data class CopyOfComponent(
    val originalCardDefinitionId: String,
    val copiedCardDefinitionId: String,
    val originalCardComponent: CardComponent? = null
) : Component

/**
 * Marks a permanent whose current copy identity is temporary and must revert to its
 * pre-copy [CardComponent] at the next end-of-turn cleanup.
 *
 * Set by "becomes a copy of … until end of turn" group-copy effects (Naga Fleshcrafter's
 * renew). The pre-copy snapshot lives on the entity's [CopyOfComponent.originalCardComponent];
 * cleanup restores it and drops both this marker and the `CopyOfComponent`. Permanent copies
 * (Mirrorform, Clone) never carry this marker, so they are unaffected by cleanup.
 */
@Serializable
data object RevertCopyAtEndOfTurnComponent : Component

/**
 * Marks a permanent whose current copy identity is temporary and must revert to its pre-copy
 * [CardComponent] at the beginning of the *next end step* — one step earlier than
 * [RevertCopyAtEndOfTurnComponent] (which waits for cleanup).
 *
 * Set by "becomes a copy of … until the next end step" group-copy effects (Niko, Light of Hope:
 * "Shards you control become copies of it until the next end step"). The revert is timed to
 * coincide with the paired "return it at the beginning of the next end step" delayed trigger, so
 * the source creature comes back and the copies wear off in the same step.
 * `CleanupPhaseManager.performNextEndStepExpiry` (invoked on entry to the end step) restores the
 * snapshot from [CopyOfComponent.originalCardComponent] and drops both this marker and the
 * `CopyOfComponent`.
 */
@Serializable
data object RevertCopyAtNextEndStepComponent : Component

/**
 * Marks a permanent whose current copy identity lasts only "for as long as [attachmentId] remains
 * attached to it" (Assimilation Aegis: "for as long as this Equipment remains attached to it, that
 * creature becomes a copy …"). The pre-copy snapshot lives on the entity's
 * [CopyOfComponent.originalCardComponent].
 *
 * A state-based check ([com.wingedsheep.engine.mechanics.sba.permanent.AttachedCopyExpiryCheck])
 * reverts the copy — restoring the snapshot and dropping both this marker and the
 * `CopyOfComponent` — the moment the [attachmentId] permanent is no longer attached to this entity
 * (the Equipment detached, moved to another creature, or left the battlefield) (CR 611.2b).
 */
@Serializable
data class CopyWhileAttachedComponent(
    val attachmentId: com.wingedsheep.sdk.model.EntityId
) : Component

/**
 * Marks a spell as uncounterable.
 * Applied to entities whose card definition has cantBeCountered = true,
 * or granted by permanents like Root Sliver.
 */
@Serializable
data object CantBeCounteredComponent : Component

/**
 * Marks a spell as uncopiable (CR 707.10).
 * Applied to entities whose card definition has cantBeCopied = true. Any effect that
 * would copy this spell on the stack creates no copy (e.g., Display of Power).
 */
@Serializable
data object CantBeCopiedComponent : Component

/**
 * Marker: this card entered a graveyard *from the battlefield* during the current turn.
 *
 * "Current turn" follows MTG turn boundaries — a new turn begins whenever a player starts
 * their turn (BeginningPhaseManager wipes the marker from every entity at the untap step
 * of every turn). The marker is set by `ZoneTransitionService` whenever a card moves
 * battlefield → graveyard, and stripped when the card leaves the graveyard so a later
 * arrival via a different path (mill, exile → graveyard, hand → graveyard) does not
 * carry the "from battlefield" claim.
 *
 * Backs `StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn` (LTR — Samwise the
 * Stouthearted's "permanent card in your graveyard that was put there from the
 * battlefield this turn" and Lobelia Sackville-Baggins's analogous exile target).
 */
@Serializable
data object PutIntoGraveyardFromBattlefieldThisTurnMarker : Component
