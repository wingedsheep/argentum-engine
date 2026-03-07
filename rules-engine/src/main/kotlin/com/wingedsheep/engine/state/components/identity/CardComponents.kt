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
 * Tracks that an entity is a copy of another card.
 * The originalCardDefinitionId preserves what the card originally was (e.g., Clone),
 * while copiedCardDefinitionId tracks what it's currently copying.
 */
@Serializable
data class CopyOfComponent(
    val originalCardDefinitionId: String,
    val copiedCardDefinitionId: String
) : Component

/**
 * Marks a spell as uncounterable.
 * Applied to entities whose card definition has cantBeCountered = true,
 * or granted by permanents like Root Sliver.
 */
@Serializable
data object CantBeCounteredComponent : Component
