package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.scripting.KeywordAbility
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * An official ruling for a Magic: The Gathering card.
 * @param date The date of the ruling (e.g., "6/8/2016")
 * @param text The ruling text explaining card interactions or clarifications
 */
@Serializable
data class Ruling(
    val date: String,
    val text: String
)

/**
 * Metadata from Scryfall for web client features like booster drafts.
 * Contains information useful for display, pricing, and card organization.
 */
@Serializable
data class ScryfallMetadata(
    val collectorNumber: String? = null,
    val rarity: Rarity = Rarity.COMMON,
    val artist: String? = null,
    val flavorText: String? = null,
    val imageUri: String? = null,
    val scryfallId: String? = null,
    val releaseDate: String? = null,
    val rulings: List<Ruling> = emptyList()
)

/**
 * Card rarity levels.
 */
@Serializable
enum class Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    MYTHIC,
    SPECIAL,
    BONUS
}

/**
 * Layout family of a card. Drives how zones (hand, stack, battlefield) render the card and
 * how the engine treats casting and characteristics.
 *
 * - [NORMAL]: a standard single-face card. Top-level [CardDefinition] fields fully describe it.
 * - [SPLIT]: a card with two or more named halves printed on one card and a shared type line
 *   (CR 709.5). Each half has its own name, mana cost, and rules text. Off-battlefield, the card
 *   has the combined characteristics of every half (709.4c). Examples: Rooms (Duskmourn), Fuse,
 *   Aftermath. Faces live in [CardDefinition.cardFaces].
 */
@Serializable
enum class CardLayout {
    NORMAL,
    SPLIT,
}

/**
 * One face of a multi-face card.
 *
 * For [CardLayout.SPLIT] cards, every face carries its own name, mana cost, type line, oracle text,
 * keywords, and per-face script. The engine reads abilities from a face's script when that face's
 * door / mode is "active" for the permanent (see CR 709.5 for Rooms, where locked halves are
 * suppressed).
 *
 * Phase 1 deliberately keeps this minimal — no spell effect / aura target / replacement effects.
 * Today it's only used by Rooms (which are permanents that don't carry a spell effect) and the
 * supported abilities are triggered, activated, and static. Adventure / Aftermath / Fuse layouts
 * will add what they need on top.
 */
@Serializable
data class CardFace(
    val name: String,
    val manaCost: ManaCost,
    val typeLine: TypeLine,
    val oracleText: String = "",
    val keywords: Set<Keyword> = emptySet(),
    val script: CardScript = CardScript.EMPTY,
)

/**
 * Complete definition of a Magic: The Gathering card.
 *
 * CardDefinition combines static attributes (name, cost, types, stats) with
 * behavioral logic (script). This is the master object that fully describes
 * a card for both the rules engine and the client.
 *
 * ## Structure
 * - **Static attributes**: name, manaCost, typeLine, creatureStats, keywords
 * - **Behavioral logic**: script (contains effects, abilities, triggers)
 * - **Metadata**: Scryfall data for display, rarity, art
 *
 * ## Philosophy
 * Cards are defined as data, not code. The script property contains pure data
 * that describes what the card does. The engine interprets this data.
 *
 * @see CardScript for behavioral logic (effects, abilities, triggers)
 */
@Serializable
data class CardDefinition(
    val name: String,
    val manaCost: ManaCost,
    val typeLine: TypeLine,
    val oracleText: String = "",
    val creatureStats: CreatureStats? = null,
    val keywords: Set<Keyword> = emptySet(),
    val flags: Set<AbilityFlag> = emptySet(),
    val keywordAbilities: List<KeywordAbility> = emptyList(),  // Parameterized keywords (Ward {2}, Protection from blue)
    val script: CardScript = CardScript.EMPTY,  // Behavioral logic (abilities, effects)
    val equipCost: ManaCost? = null,  // For Equipment cards
    val oracleId: String? = null,
    val setCode: String? = null,
    val backFace: CardDefinition? = null,  // For double-faced cards
    val metadata: ScryfallMetadata = ScryfallMetadata(),  // Scryfall metadata for web client
    val startingLoyalty: Int? = null,  // For planeswalkers
    val legalFormats: Set<DeckFormat> = emptySet(),  // Formats in which the card is legal (Scryfall-sourced)
    val colorIdentityOverride: Set<Color>? = null,  // Authoritative Scryfall color identity; null = derive from heuristic
    val layout: CardLayout = CardLayout.NORMAL,
    val cardFaces: List<CardFace> = emptyList()  // Populated for non-NORMAL layouts (e.g. SPLIT Rooms)
) {
    init {
        if (typeLine.isCreature) {
            requireNotNull(creatureStats) { "Creature cards must have power/toughness: $name" }
        }
    }

    val cmc: Int get() = manaCost.cmc

    val colors: Set<Color> get() = manaCost.colors

    /**
     * Color identity per CR 903.4: the colors of any mana symbols in the card's mana cost or
     * rules text, plus any colors defined by characteristic-defining abilities or color
     * indicators. Used by Commander/Brawl deck-construction to determine which cards may share
     * a deck with a given commander.
     *
     * Implementation:
     *  - Mana cost contributes its colored symbols (covered by [ManaCost.colors]).
     *  - Oracle text is scanned for `{W}`, `{U}`, `{B}`, `{R}`, `{G}` symbols, plus hybrid
     *    (`{W/U}`, `{2/W}`) and Phyrexian (`{W/P}`) variants. This catches activated abilities
     *    with off-color activation costs (e.g. an artifact whose only colored symbol lives in
     *    `{2}{B}, {T}: …` adds black to its identity).
     *  - Basic land subtypes contribute their associated color: Plains→W, Island→U, Swamp→B,
     *    Mountain→R, Forest→G. This applies to any land with the subtype, not just to cards
     *    with the basic supertype, so dual lands like Tundra (Land — Plains Island) correctly
     *    pick up both colors.
     *
     * Not yet modelled: explicit color indicators (rule 204) — there's no field on
     * [CardDefinition] for them today. When that's added, fold the indicator's colors in here.
     *
     * If [colorIdentityOverride] is set (typically from the Scryfall bulk-data sync), that value
     * is authoritative and the heuristic is skipped. Use the override for cards where the
     * heuristic differs from Scryfall (color indicators, devoid, hybrid quirks, etc.).
     */
    val colorIdentity: Set<Color>
        get() {
            colorIdentityOverride?.let { return it }
            val identity = manaCost.colors.toMutableSet()
            if (oracleText.isNotBlank()) {
                for (match in COLOR_SYMBOL_REGEX.findAll(oracleText)) {
                    for (ch in match.groupValues[1]) {
                        Color.fromSymbol(ch)?.let(identity::add)
                    }
                }
            }
            for (subtype in typeLine.subtypes) {
                BASIC_LAND_SUBTYPE_TO_COLOR[subtype]?.let(identity::add)
            }
            return identity
        }

    val isCreature: Boolean get() = typeLine.isCreature
    val isLand: Boolean get() = typeLine.isLand
    val isSorcery: Boolean get() = typeLine.isSorcery
    val isInstant: Boolean get() = typeLine.isInstant
    val isEnchantment: Boolean get() = typeLine.isEnchantment
    val isArtifact: Boolean get() = typeLine.isArtifact
    val isAura: Boolean get() = typeLine.isAura
    val isEquipment: Boolean get() = typeLine.isEquipment
    val isPermanent: Boolean get() = typeLine.isPermanent
    val isDoubleFaced: Boolean get() = backFace != null
    val isSplit: Boolean get() = layout == CardLayout.SPLIT
    val isPlaneswalker: Boolean get() = CardType.PLANESWALKER in typeLine.cardTypes
    val isClass: Boolean get() = typeLine.isClass
    val isSaga: Boolean get() = typeLine.isSaga

    fun hasKeyword(keyword: Keyword): Boolean = keyword in keywords

    // =========================================================================
    // Script Convenience Properties
    // =========================================================================

    /** Whether this card has any scripted behavior beyond being a vanilla permanent */
    val hasBehavior: Boolean get() = script.hasBehavior

    /** The effect when this spell resolves (for instants/sorceries) */
    val spellEffect get() = script.spellEffect

    /** Triggered abilities on this card */
    val triggeredAbilities get() = script.triggeredAbilities

    /** Activated abilities on this card */
    val activatedAbilities get() = script.activatedAbilities

    /** Static abilities (continuous effects) on this card */
    val staticAbilities get() = script.staticAbilities

    /** Target requirements when casting this spell */
    val targetRequirements get() = script.targetRequirements

    /** Class level abilities */
    val classLevels get() = script.classLevels

    /** The maximum class level, or null if not a Class */
    val maxClassLevel: Int? get() = script.maxClassLevel

    /** Saga chapter abilities */
    val sagaChapters get() = script.sagaChapters

    /** The final chapter number for Sagas, or null if not a Saga */
    val finalChapter: Int? get() = script.sagaChapters.maxOfOrNull { it.chapter }

    /** Whether this spell requires targets when cast */
    val requiresTargets: Boolean get() = script.requiresTargets

    /** Whether this card has additional costs beyond mana */
    val hasAdditionalCosts: Boolean get() = script.hasAdditionalCosts

    override fun toString(): String = buildString {
        append(name)
        if (manaCost.symbols.isNotEmpty()) {
            append(" ")
            append(manaCost)
        }
        append("\n")
        append(typeLine)
        if (oracleText.isNotBlank()) {
            append("\n")
            append(oracleText)
        }
        creatureStats?.let {
            append("\n")
            append(it)
        }
    }

    companion object {
        /**
         * Captures color letters inside any mana-symbol braces in oracle text. The pattern
         * matches a single `{...}` group whose contents contain at least one of W/U/B/R/G,
         * then we pull every color letter from the captured contents — that handles hybrid
         * (`{W/U}`, `{2/G}`), Phyrexian (`{W/P}`), and plain colored mana (`{W}`) in one pass.
         * Generic, X, and colorless symbols don't match (they have no color letters).
         */
        private val COLOR_SYMBOL_REGEX = Regex("""\{([^}]*[WUBRG][^}]*)\}""")

        /**
         * Basic land subtypes contribute their associated color to identity per CR 903.4 — the
         * land's intrinsic mana ability counts as a colored mana symbol in the rules text. The
         * map covers any land carrying these subtypes, not only basics, so dual lands like
         * Tundra (Land — Plains Island) correctly pick up both colors.
         */
        private val BASIC_LAND_SUBTYPE_TO_COLOR: Map<Subtype, Color> = mapOf(
            Subtype.PLAINS to Color.WHITE,
            Subtype.ISLAND to Color.BLUE,
            Subtype.SWAMP to Color.BLACK,
            Subtype.MOUNTAIN to Color.RED,
            Subtype.FOREST to Color.GREEN,
        )

        fun creature(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            power: Int,
            toughness: Int,
            oracleText: String = "",
            keywords: Set<Keyword> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.CREATURE),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            creatureStats = CreatureStats(power, toughness),
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        fun sorcery(
            name: String,
            manaCost: ManaCost,
            oracleText: String,
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.sorcery(),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun instant(
            name: String,
            manaCost: ManaCost,
            oracleText: String,
            script: CardScript = CardScript.EMPTY
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.instant(),
            oracleText = oracleText,
            script = script
        )

        fun basicLand(
            name: String,
            subtype: Subtype,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.basicLand(subtype),
            metadata = metadata
            // Note: Basic lands have implicit mana abilities inferred from subtypes
        )

        fun enchantment(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            subtypes: Set<Subtype> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ENCHANTMENT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun aura(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ENCHANTMENT),
                subtypes = setOf(Subtype.AURA)
            ),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun artifact(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            subtypes: Set<Subtype> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun equipment(
            name: String,
            manaCost: ManaCost,
            equipCost: ManaCost,
            oracleText: String = "",
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT),
                subtypes = setOf(Subtype.EQUIPMENT)
            ),
            oracleText = oracleText,
            script = script,
            equipCost = equipCost,
            metadata = metadata
        )

        fun artifactCreature(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            power: Int,
            toughness: Int,
            oracleText: String = "",
            keywords: Set<Keyword> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            creatureStats = CreatureStats(power, toughness),
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        /**
         * Creates a double-faced transforming creature card.
         * @param frontFace The front face definition (must be a creature)
         * @param backFace The back face definition (must be a creature)
         */
        fun doubleFacedCreature(
            frontFace: CardDefinition,
            backFace: CardDefinition
        ): CardDefinition {
            require(frontFace.isCreature) { "Front face must be a creature" }
            require(backFace.isCreature) { "Back face must be a creature" }
            return frontFace.copy(backFace = backFace)
        }

        /**
         * Creates a double-faced transforming permanent of any permanent type.
         * Use this for non-creature TDFCs (e.g., Incubator tokens whose front face is
         * an artifact and back face is an artifact creature).
         */
        fun doubleFacedPermanent(
            frontFace: CardDefinition,
            backFace: CardDefinition
        ): CardDefinition {
            require(frontFace.isPermanent) { "Front face must be a permanent: ${frontFace.name}" }
            require(backFace.isPermanent) { "Back face must be a permanent: ${backFace.name}" }
            return frontFace.copy(backFace = backFace)
        }

        /**
         * Creates a planeswalker card.
         * @param name Card name
         * @param manaCost Mana cost
         * @param subtypes Planeswalker subtypes (e.g., Ajani, Jace)
         * @param startingLoyalty Starting loyalty counters
         * @param oracleText Oracle text describing abilities
         * @param supertypes Supertypes (typically Legendary)
         * @param metadata Scryfall metadata
         */
        fun planeswalker(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            startingLoyalty: Int,
            oracleText: String = "",
            supertypes: Set<Supertype> = setOf(Supertype.LEGENDARY),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.PLANESWALKER),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            script = script,
            startingLoyalty = startingLoyalty,
            metadata = metadata
        )

        /**
         * Creates an instant card with metadata and optional script.
         */
        fun instant(
            name: String,
            manaCost: ManaCost,
            oracleText: String,
            keywords: Set<Keyword> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.instant(),
            oracleText = oracleText,
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        /**
         * Creates a Kindred Instant (instant with creature subtypes).
         * Used for spells like Crib Swap that have Changeling.
         */
        fun kindredInstant(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            oracleText: String,
            keywords: Set<Keyword> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                cardTypes = setOf(CardType.KINDRED, CardType.INSTANT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        /**
         * Creates a Kindred Enchantment (enchantment with creature subtypes).
         */
        fun kindredEnchantment(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            oracleText: String,
            keywords: Set<Keyword> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                cardTypes = setOf(CardType.KINDRED, CardType.ENCHANTMENT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            keywords = keywords,
            script = script,
            metadata = metadata
        )
    }
}
