package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Chosen Token Effects
// =============================================================================

/**
 * Create a creature token using the chosen color and creature type from the source permanent.
 * Power/toughness are determined by dynamic amounts evaluated at resolution time.
 *
 * Used for Riptide Replicator: "Create an X/X creature token of the chosen color and type,
 * where X is the number of charge counters on Riptide Replicator."
 *
 * Reads ChosenColorComponent and ChosenCreatureTypeComponent from the source.
 *
 * @property dynamicPower Dynamic amount for the token's power
 * @property dynamicToughness Dynamic amount for the token's toughness
 */
@SerialName("CreateChosenToken")
@Serializable
data class CreateChosenTokenEffect(
    val dynamicPower: DynamicAmount,
    val dynamicToughness: DynamicAmount
) : Effect {
    override val description: String =
        "Create an ${dynamicPower.description}/${dynamicToughness.description} creature token of the chosen color and type"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = dynamicPower.applyTextReplacement(replacer)
        val newToughness = dynamicToughness.applyTextReplacement(replacer)
        return if (newPower !== dynamicPower || newToughness !== dynamicToughness) copy(dynamicPower = newPower, dynamicToughness = newToughness) else this
    }
}

// =============================================================================
// Token Effects
// =============================================================================

/**
 * Create token effect.
 * "Create a 1/1 white Soldier creature token" or "Create X 1/1 green Insect creature tokens"
 *
 * Supports both fixed and dynamic counts via [DynamicAmount].
 *
 * @property count Number of tokens to create (fixed or dynamic)
 * @property power Token power
 * @property toughness Token toughness
 * @property colors Token colors
 * @property creatureTypes Token creature types (e.g., "Soldier", "Kithkin")
 * @property keywords Keywords the token has (e.g., flying, vigilance)
 * @property name Optional token name (defaults to creature types + "Token")
 * @property imageUri Optional image URI for the token artwork
 * @property controller Who receives the token. Null means the spell/ability controller (default).
 *   Use [EffectTarget.TargetController] to give the token to the controller of a targeted permanent.
 * @property exileAtStep If set, create delayed triggers to exile the created tokens at this step.
 *   Used for "Create tokens... Exile them at the beginning of the next end step" patterns (e.g., Valduk, Kiki-Jiki).
 */
@SerialName("CreateToken")
@Serializable
data class CreateTokenEffect(
    val count: DynamicAmount = DynamicAmount.Fixed(1),
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>,
    val keywords: Set<Keyword> = emptySet(),
    val name: String? = null,
    val imageUri: String? = null,
    val controller: EffectTarget? = null,
    val dynamicPower: DynamicAmount? = null,
    val dynamicToughness: DynamicAmount? = null,
    val tapped: Boolean = false,
    val attacking: Boolean = false,
    val legendary: Boolean = false,
    val artifactToken: Boolean = false,
    val staticAbilities: List<StaticAbility> = emptyList(),
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),
    val exileAtStep: Step? = null
) : Effect {
    constructor(
        count: Int,
        power: Int,
        toughness: Int,
        colors: Set<Color>,
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet(),
        name: String? = null,
        imageUri: String? = null,
        controller: EffectTarget? = null,
        legendary: Boolean = false
    ) : this(DynamicAmount.Fixed(count), power, toughness, colors, creatureTypes, keywords, name, imageUri, controller, legendary = legendary, artifactToken = false)

    override val description: String = buildString {
        append("Create ")
        when (val c = count) {
            is DynamicAmount.Fixed -> {
                append(if (c.amount == 1) "a" else "${c.amount}")
                append(" $power/$toughness ")
                append(colors.joinToString(" and ") { it.displayName.lowercase() })
                append(" ")
                append(creatureTypes.joinToString(" "))
                append(" creature token")
                if (c.amount != 1) append("s")
            }
            else -> {
                append(c.description)
                append(" $power/$toughness ")
                append(colors.joinToString(" and ") { it.displayName.lowercase() })
                append(" ")
                append(creatureTypes.joinToString(" "))
                append(" creature tokens")
            }
        }
        if (keywords.isNotEmpty()) {
            append(" with ")
            append(keywords.joinToString(", ") { it.name.lowercase() })
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCount = count.applyTextReplacement(replacer)
        val newTypes = creatureTypes.map { replacer.replaceCreatureType(it) }.toSet()
        return if (newCount !== count || newTypes != creatureTypes) copy(count = newCount, creatureTypes = newTypes) else this
    }
}

/**
 * Create Treasure artifact tokens.
 * Treasure tokens have "{T}, Sacrifice this artifact: Add one mana of any color."
 */
@SerialName("CreateTreasureTokens")
@Serializable
data class CreateTreasureTokensEffect(
    val count: Int = 1
) : Effect {
    override val description: String = if (count == 1) {
        "Create a Treasure token"
    } else {
        "Create $count Treasure tokens"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Create Food artifact tokens.
 * Food tokens have "{2}, {T}, Sacrifice this artifact: You gain 3 life."
 *
 * @property count Number of Food tokens to create
 * @property controller Who controls the created tokens (null = spell controller)
 */
@SerialName("CreateFoodTokens")
@Serializable
data class CreateFoodTokensEffect(
    val count: Int = 1,
    val controller: EffectTarget? = null
) : Effect {
    override val description: String = if (count == 1) {
        "Create a Food token"
    } else {
        "Create $count Food tokens"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Create Lander artifact tokens.
 * Lander tokens are artifacts with "{2}, {T}, Sacrifice this token: Search your library for a
 * basic land card, put it onto the battlefield tapped, then shuffle."
 *
 * @property count Number of Lander tokens to create
 * @property controller Who controls the created tokens (null = spell controller)
 */
@SerialName("CreateLanderTokens")
@Serializable
data class CreateLanderTokensEffect(
    val count: Int = 1,
    val controller: EffectTarget? = null
) : Effect {
    override val description: String = if (count == 1) {
        "Create a Lander token"
    } else {
        "Create $count Lander tokens"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Create a token that's a copy of the source permanent.
 * "Create a token that's a copy of Mishra's Self-Replicator."
 *
 * The token copies the source's copiable values (name, mana cost, colors, types,
 * abilities, power/toughness) from its CardComponent and CardDefinition. The token
 * enters with summoning sickness and is marked as a token (TokenComponent).
 *
 * The source is determined by [EffectTarget.Self] — the permanent that has this ability.
 */
@SerialName("CreateTokenCopyOfSource")
@Serializable
data class CreateTokenCopyOfSourceEffect(
    val count: Int = 1,
    /** Override the token's base power (null = copy source's power) */
    val overridePower: Int? = null,
    /** Override the token's base toughness (null = copy source's toughness) */
    val overrideToughness: Int? = null
) : Effect {
    override val description: String = buildString {
        append("Create ")
        if (count == 1) append("a token that's a copy of this creature")
        else append("$count tokens that are copies of this creature")
        if (overridePower != null && overrideToughness != null) {
            append(", except it's $overridePower/$overrideToughness")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Create a token that's a copy of an equipped/attached creature.
 * Used for equipment cards like Helm of the Host that create copies of the equipped creature.
 *
 * The source (this equipment) looks up its attached creature via [AttachedToComponent]
 * and creates a token copy of that creature. The token can optionally have legendary
 * removed and gain haste.
 *
 * @property removeLegendary If true, the token copy is not legendary
 * @property grantHaste If true, the token gains haste
 */
@SerialName("CreateTokenCopyOfEquippedCreature")
@Serializable
data class CreateTokenCopyOfEquippedCreatureEffect(
    val removeLegendary: Boolean = false,
    val grantHaste: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Create a token that's a copy of equipped creature")
        if (removeLegendary) append(", except the token isn't legendary")
        if (grantHaste) append(". That token gains haste")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Effect that can be activated from the graveyard.
 * Used for cards like Goldmeadow Nomad with graveyard abilities.
 * Note: This is typically handled as an activated ability, not a spell effect.
 */
@SerialName("CreateTokenFromGraveyard")
@Serializable
data class CreateTokenFromGraveyardEffect(
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>
) : Effect {
    override val description: String = buildString {
        append("Create a $power/$toughness ")
        append(colors.joinToString(" and ") { it.displayName.lowercase() })
        append(" ")
        append(creatureTypes.joinToString(" "))
        append(" creature token")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newTypes = creatureTypes.map { replacer.replaceCreatureType(it) }.toSet()
        return if (newTypes != creatureTypes) copy(creatureTypes = newTypes) else this
    }
}
