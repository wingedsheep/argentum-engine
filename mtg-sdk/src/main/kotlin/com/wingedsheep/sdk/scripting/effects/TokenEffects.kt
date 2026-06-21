package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * @property sacrificeAtStep If set, create delayed triggers to sacrifice the created tokens at this step.
 *   The sacrifice sibling of [exileAtStep] — tokens go to the graveyard (firing dies/leaves and
 *   "whenever you sacrifice" triggers) rather than being exiled. Used by Mobilize N
 *   ("Sacrifice those tokens at the beginning of the next end step").
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
    /**
     * When true the token is an *enchantment* creature — its type line gains the ENCHANTMENT card
     * type alongside CREATURE (e.g. Duskmourn's "1/1 white Glimmer enchantment creature token").
     * The sibling of [artifactToken] for the enchantment-creature shape; both may be set at once
     * (an artifact enchantment creature token).
     */
    val enchantmentToken: Boolean = false,
    val staticAbilities: List<StaticAbility> = emptyList(),
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),
    /**
     * Activated abilities the token has, e.g. a Mercenary token with
     * `"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery."`
     * (Mourner's Surprise). Each is granted to every created token at resolution via
     * `GameState.grantedActivatedAbilities` (permanent duration) — the same path the engine
     * already uses for granted activated abilities, so the legal-action enumerator and
     * `ActivateAbilityHandler` pick them up for free. The sibling of [staticAbilities] /
     * [triggeredAbilities] for the activated-ability shape.
     */
    val activatedAbilities: List<ActivatedAbility> = emptyList(),
    val exileAtStep: Step? = null,
    val sacrificeAtStep: Step? = null,
    /** Counters to place on the token when it enters the battlefield. */
    val initialCounters: Map<String, Int> = emptyMap(),
    /**
     * If set, the token's color is the color the source locked into this cast-choice slot
     * (rather than the fixed [colors]) — e.g. Riptide Replicator "of the chosen color".
     * Read from the source's cast-choices bag at resolution; null means use [colors].
     */
    val colorsFromChoice: com.wingedsheep.sdk.scripting.ChoiceSlot? = null,
    /**
     * If set, the token's creature type is the type the source locked into this cast-choice slot
     * (rather than the fixed [creatureTypes]) — e.g. Riptide Replicator "of the chosen type".
     * Read from the source's cast-choices bag at resolution; null means use [creatureTypes].
     */
    val creatureTypesFromChoice: com.wingedsheep.sdk.scripting.ChoiceSlot? = null
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
        val pt = if (dynamicPower != null && dynamicToughness != null) {
            "${dynamicPower.description}/${dynamicToughness.description}"
        } else "$power/$toughness"
        val colorWord = if (colorsFromChoice != null) "the chosen color"
            else colors.joinToString(" and ") { it.displayName.lowercase() }
        val typeWord = if (creatureTypesFromChoice != null) "the chosen type"
            else creatureTypes.joinToString(" ")
        // "enchantment creature" — the extra card type precedes "creature". (artifactToken is
        // intentionally not rendered here to keep existing artifact-token descriptions stable.)
        val cardTypeWord = if (enchantmentToken) "enchantment creature" else "creature"
        append("Create ")
        when (val c = count) {
            is DynamicAmount.Fixed -> {
                append(if (c.amount == 1) "a" else "${c.amount}")
                append(" $pt $colorWord $typeWord $cardTypeWord token")
                if (c.amount != 1) append("s")
            }
            else -> {
                append("${c.description} $pt $colorWord $typeWord $cardTypeWord tokens")
            }
        }
        if (keywords.isNotEmpty()) {
            append(" with ")
            append(keywords.joinToString(", ") { it.name.lowercase() })
        }
        // Render granted activated abilities as quoted reminder text, e.g.
        // `with "{T}: Target creature you control gets +1/+0 until end of turn."`.
        for (ability in activatedAbilities) {
            append(if (keywords.isEmpty()) " with " else " and ")
            append("\"${ability.description}\"")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCount = count.applyTextReplacement(replacer)
        val newTypes = creatureTypes.map { replacer.replaceCreatureType(it) }.toSet()
        val newAbilities = activatedAbilities.map { it.applyTextReplacement(replacer) }
        val abilitiesChanged = newAbilities.zip(activatedAbilities).any { (a, b) -> a !== b }
        return if (newCount !== count || newTypes != creatureTypes || abilitiesChanged)
            copy(count = newCount, creatureTypes = newTypes, activatedAbilities = newAbilities) else this
    }
}

/**
 * Pipeline collection name under which [CreatePredefinedTokenEffect] publishes the
 * entity IDs of the tokens it just created. Sibling effects in a [CompositeEffect]
 * can address those tokens via `EffectTarget.PipelineTarget(CREATED_TOKENS, index)`.
 *
 * Used by composition patterns like Incubate, where one atomic creates the token
 * and the next atomic puts counters on it.
 */
const val CREATED_TOKENS = "createdTokens"

/**
 * Create predefined artifact tokens (Treasure, Food, Lander, etc.).
 *
 * The [tokenType] must match a CardDefinition registered in PredefinedTokens. The engine
 * looks up the token's type line, abilities, and metadata from the CardDefinition at runtime.
 *
 * Created token entity IDs are published to the pipeline under [CREATED_TOKENS] so
 * sibling effects in a composite (e.g. AddCounters for Incubate) can address them.
 *
 * To add a new predefined token type:
 * 1. Add a CardDefinition to `PredefinedTokens.kt` (mtg-sets)
 * 2. Add a facade method to `Effects.kt` (e.g., `Effects.CreateClue()`)
 *
 * @property tokenType Name of the predefined token (must match a registered CardDefinition)
 * @property count Number of tokens to create
 * @property controller Who controls the created tokens (null = spell controller)
 * @property tapped Whether the created tokens enter the battlefield tapped
 */
@SerialName("CreatePredefinedToken")
@Serializable
data class CreatePredefinedTokenEffect(
    val tokenType: String,
    val count: Int = 1,
    val controller: EffectTarget? = null,
    val tapped: Boolean = false,
    /**
     * If set, the engine evaluates this at resolution time and uses the result as the
     * token count instead of [count]. Used by Lobelia Sackville-Baggins ("create X
     * Treasure tokens, where X is the exiled card's power").
     */
    val dynamicCount: DynamicAmount? = null
) : Effect {
    override val description: String = buildString {
        append(if (dynamicCount != null) "Create " else if (count == 1) "Create a " else "Create $count ")
        if (dynamicCount != null) append("X ")
        if (tapped) append("tapped ")
        append(tokenType)
        append(if (dynamicCount != null || count != 1) " tokens" else " token")
    }
}

/**
 * Create an Aura Role token of the specified type attached to a target creature.
 *
 * Role tokens are Enchantment — Aura Role tokens defined in PredefinedTokens.
 * Role replacement rule: if the target already has a Role controlled by the same
 * player, the old Role is put into the graveyard before the new one enters.
 *
 * @property roleName The name of the Role token (e.g., "Sorcerer Role")
 * @property target The creature to attach the Role to
 */
@SerialName("CreateRoleToken")
@Serializable
data class CreateRoleTokenEffect(
    val roleName: String,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Create a $roleName token attached to ${target.description}"
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
    val overrideToughness: Int? = null,
    /** If set, create delayed triggers to exile the created tokens at this step. */
    val exileAtStep: Step? = null,
    /**
     * Extra card types to union onto the token's type line on top of the source's types.
     * Use the [com.wingedsheep.sdk.core.CardType] `name` (e.g. `"ARTIFACT"`).
     *
     * Models the "except it's a [type] in addition to its other types" copy clause —
     * the token has every type the source had, plus the listed types. First used for
     * Vaultborn Tyrant's death trigger ("create a token that's a copy of it, except
     * it's an artifact in addition to its other types"); any future copy-token effect
     * with the same shape can reuse the same field.
     */
    val addCardTypes: Set<String> = emptySet()
) : Effect {
    override val description: String = buildString {
        append("Create ")
        if (count == 1) append("a token that's a copy of this creature")
        else append("$count tokens that are copies of this creature")
        val excepts = mutableListOf<String>()
        if (overridePower != null && overrideToughness != null) {
            excepts.add("it's $overridePower/$overrideToughness")
        }
        if (addCardTypes.isNotEmpty()) {
            val typeWords = addCardTypes.map { it.lowercase() }
            excepts.add("it's ${if (count == 1) "an " else ""}${typeWords.joinToString(" ")} in addition to its other types")
        }
        if (excepts.isNotEmpty()) {
            append(", except ")
            append(excepts.joinToString(" and "))
        }
        if (exileAtStep != null) {
            append(". Exile ${if (count == 1) "it" else "them"} at the beginning of the next ${exileAtStep.displayName}")
        }
    }
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
}

/**
 * Choose a permanent you control matching [filter], then create a token that's a copy of it.
 * "Choose an artifact or creature you control. Create a token that's a copy of it."
 *
 * The choice is made during resolution (not at cast time). The executor finds matching
 * permanents the controller controls, presents a selection decision, and creates a
 * token copy of the chosen permanent.
 *
 * @property filter Which permanents can be chosen (e.g., artifact or creature)
 */
@SerialName("CreateTokenCopyOfChosenPermanent")
@Serializable
data class CreateTokenCopyOfChosenPermanentEffect(
    val filter: GameObjectFilter = GameObjectFilter.Permanent
) : Effect {
    override val description: String =
        "Choose a ${filter.description} you control. Create a token that's a copy of it"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Create token copies of a targeted permanent.
 * "Create X tokens that are copies of target token you control."
 *
 * The target is chosen at cast time via [EffectTarget.ContextTarget]. At resolution,
 * creates [count] token copies of that targeted permanent.
 *
 * @property target The targeted permanent to copy (usually ContextTarget(0))
 * @property count Number of copies to create (supports DynamicAmount for X spells)
 * @property overridePower Override the token's base power (null = copy source's power)
 * @property overrideToughness Override the token's base toughness (null = copy source's toughness)
 * @property tapped Whether the copies enter tapped
 * @property attacking Whether the copies enter attacking
 * @property triggeredAbilities Additional triggered abilities granted to the copies
 */
@SerialName("CreateTokenCopyOfTarget")
@Serializable
data class CreateTokenCopyOfTargetEffect(
    val target: EffectTarget,
    val count: DynamicAmount = DynamicAmount.Fixed(1),
    val overridePower: Int? = null,
    val overrideToughness: Int? = null,
    val tapped: Boolean = false,
    val attacking: Boolean = false,
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),
    /** Keywords granted to the token copy in addition to those copied from the source. */
    val addedKeywords: Set<Keyword> = emptySet(),
    /** Supertypes added to the token copy's type line (e.g., LEGENDARY for Adagia, Windswept Bastion). */
    val addedSupertypes: Set<Supertype> = emptySet(),
    /** Supertypes stripped from the token copy's type line (e.g., LEGENDARY for Impostor Syndrome). */
    val removedSupertypes: Set<Supertype> = emptySet(),
    /**
     * Replaces the token copy's colors outright (e.g., black for Ardyn, the Usurper's
     * "a 5/5 black Demon"). Null leaves the copied card's colors untouched.
     */
    val overrideColors: Set<Color>? = null,
    /**
     * Colors unioned onto the token copy's colors *in addition* to those copied (e.g. red for
     * The Jolly Balloon Man's "a 1/1 red Balloon creature in addition to its other colors and
     * types"). Distinct from [overrideColors] (which replaces). Ignored when [overrideColors] is
     * set.
     */
    val addedColors: Set<Color> = emptySet(),
    /**
     * Replaces the token copy's subtypes outright (e.g., Demon for Ardyn, the Usurper).
     * Null leaves the copied card's subtypes untouched.
     */
    val overrideSubtypes: Set<Subtype>? = null,
    /**
     * Subtypes unioned onto the token copy's subtypes *in addition* to those copied
     * (e.g., Golem for Nexus of Becoming's "a 3/3 Golem artifact creature in addition to
     * its other types"). Distinct from [overrideSubtypes] (which replaces). Ignored when
     * [overrideSubtypes] is set.
     */
    val addedSubtypes: Set<Subtype> = emptySet(),
    /**
     * Replaces the token copy's card types outright (e.g. Shelob, Child of Ungoliant's copy "is a
     * Food artifact ... and it loses all other card types" → {ARTIFACT}). Null leaves the copied
     * card's card types untouched. Note: when this drops CREATURE, the copy is no longer a creature,
     * so it won't enter attacking and copies no P/T meaning.
     */
    val overrideCardTypes: Set<CardType>? = null,
    /**
     * Activated abilities granted to the token copy in addition to those copied from the source
     * (e.g. Shelob, Child of Ungoliant grants the Food sacrifice ability
     * "{2}, {T}, Sacrifice this token: You gain 3 life"). Mirrors [CreateTokenEffect.activatedAbilities].
     */
    val activatedAbilities: List<ActivatedAbility> = emptyList(),
    /**
     * If set, create delayed triggers to sacrifice each created token copy at this step
     * (the sacrifice sibling of [CreateTokenEffect.sacrificeAtStep]). Used for "create a
     * tapped token copy ... at the beginning of your next end step, sacrifice those tokens"
     * (Mardu Siegebreaker).
     */
    val sacrificeAtStep: Step? = null,
    /**
     * When [sacrificeAtStep] is set, gate the delayed sacrifice trigger to fire only on the
     * controller's turn — i.e. "at the beginning of *your* next end step" rather than the
     * very next end step of any player.
     */
    val sacrificeOnlyOnControllersTurn: Boolean = false,
    /**
     * Extra card types to union onto the token's type line on top of the copied permanent's
     * types. Use the [com.wingedsheep.sdk.core.CardType] `name` (e.g. `"ARTIFACT"`).
     *
     * Models the "except it's a [type] in addition to its other types" copy clause — the token
     * has every type the copied permanent had, plus the listed types. The targeted sibling of
     * [CreateTokenCopyOfSourceEffect.addCardTypes]. First used for Molten Duplication ("a copy of
     * target artifact or creature you control, except it's an artifact in addition to its other
     * types"); any future targeted copy-token effect with the same shape can reuse the field.
     */
    val addCardTypes: Set<String> = emptySet(),
    /**
     * If set, create delayed triggers to exile each created token copy at this step. The exile
     * sibling of [sacrificeAtStep], used for "create a token copy ... at the beginning of the
     * next end step, exile that token" (Sauron, the Necromancer). Unlike [sacrificeAtStep] the
     * token is *exiled* rather than sacrificed, and the firing step is the next matching step of
     * any player's turn ("the next end step", not "your next end step").
     */
    val exileAtStep: Step? = null,
    /**
     * When [exileAtStep] is set, the delayed exile is skipped if the source permanent is the
     * controller's Ring-bearer at the time the trigger fires (CR 701.54e) — i.e. "exile that
     * token unless [source] is your Ring-bearer" (Sauron, the Necromancer).
     */
    val exileUnlessSourceIsRingBearer: Boolean = false,
    /**
     * The player who creates (and so controls and owns) the token copy. `null` defaults to the
     * effect's controller — the normal "you create a token that's a copy of …" case. Set it to a
     * resolved player target for "**Target player** creates a token that's a copy of target
     * creature you control" (Echocasting Symposium): the chosen permanent is copied, but the new
     * token is put onto the battlefield under the named player's control. Mirrors
     * [CreateTokenEffect.controller].
     */
    val controller: EffectTarget? = null
) : Effect {
    override val description: String = buildString {
        append("Create ${count.description} token copies of target permanent")
        // Merge any copiable-value overrides into one clause, e.g. "except it's a 5/5 black Demon".
        val pt = if (overridePower != null && overrideToughness != null) "$overridePower/$overrideToughness" else null
        val color = overrideColors?.joinToString(" ") { it.displayName.lowercase() }
        val type = overrideSubtypes?.joinToString(" ") { it.value }
        val descriptor = listOfNotNull(pt, color, type).joinToString(" ")
        if (descriptor.isNotEmpty()) {
            val pronoun = if (count == DynamicAmount.Fixed(1)) "it's" else "they're"
            // Article only when the descriptor ends in a type noun ("a Demon"); a bare P/T or
            // color reads fine without one ("it's 5/5", "it's black").
            val article = if (type != null) {
                if (descriptor.first().lowercaseChar() in "aeiou") "an " else "a "
            } else ""
            append(", except $pronoun $article$descriptor")
        }
        if (tapped) append(" tapped")
        if (attacking) append(" and attacking")
        if (removedSupertypes.isNotEmpty()) {
            append(", except ${if (count == DynamicAmount.Fixed(1)) "it's" else "they're"} not ${
                removedSupertypes.joinToString(" ") { it.displayName.lowercase() }
            }")
        }
        if (addedSupertypes.isNotEmpty()) {
            append(", except ${if (count == DynamicAmount.Fixed(1)) "it's" else "they're"} ${
                addedSupertypes.joinToString(" ") { it.displayName.lowercase() }
            }")
        }
        if (addCardTypes.isNotEmpty() || addedSubtypes.isNotEmpty()) {
            val pronoun = if (count == DynamicAmount.Fixed(1)) "it's" else "they're"
            val typeWords = (addedSubtypes.map { it.value } + addCardTypes.map { it.lowercase() })
                .joinToString(" ")
            val article = if (count == DynamicAmount.Fixed(1)) {
                if (typeWords.first().lowercaseChar() in "aeiou") "an " else "a "
            } else ""
            append(", except $pronoun $article$typeWords in addition to its other types")
        }
        if (overrideCardTypes != null) {
            append(", except ${if (count == DynamicAmount.Fixed(1)) "it's" else "they're"} ${
                overrideCardTypes.joinToString(" ") { it.displayName.lowercase() }
            } and loses all other card types")
        }
        if (addedKeywords.isNotEmpty()) {
            append(" with ${addedKeywords.joinToString(", ") { it.displayName.lowercase() }}")
        }
        if (exileAtStep != null) {
            val pronoun = if (count == DynamicAmount.Fixed(1)) "that token" else "those tokens"
            append(". At the beginning of the next ${exileAtStep.name.lowercase()} step, exile $pronoun")
            if (exileUnlessSourceIsRingBearer) append(" unless this creature is your Ring-bearer")
        }
    }
}

/**
 * Create a token that's a copy of a *randomly chosen* creature card whose mana value equals
 * [manaValue]. The Momir Basic avatar's payoff
 * (<https://mtg.fandom.com/wiki/Momir>): "Create a token that's a copy of a randomly chosen
 * creature card with mana value X."
 *
 * The candidate pool is the set-scoped creature list carried on the active
 * [com.wingedsheep.sdk.core.Format.MomirBasic.eligibleCreatureNames] — the executor filters it to
 * the cards whose mana value equals the resolved [manaValue], then picks one with the game's
 * seeded RNG (replay-stable). If no creature has that mana value, nothing happens (the cost was
 * still paid). The minted token's own `{X}` reads 0 (it never went on the stack).
 *
 * Parameterized over [manaValue] (a [DynamicAmount]) rather than baking in "X" so the same
 * primitive serves any "random creature of mana value N" effect; the avatar passes
 * [DynamicAmount.XValue].
 */
@SerialName("CreateRandomCreatureTokenWithManaValue")
@Serializable
data class CreateRandomCreatureTokenWithManaValueEffect(
    val manaValue: DynamicAmount,
) : Effect {
    override val description: String =
        "Create a token that's a copy of a randomly chosen creature card with mana value ${manaValue.description}"
}
