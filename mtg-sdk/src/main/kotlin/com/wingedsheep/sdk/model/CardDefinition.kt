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
    /**
     * Display-only art selected from the permanent's projected creature subtypes. This lets art
     * follow continuous type-changing effects without making the client infer game rules.
     */
    val imageUriByCreatureSubtype: Map<String, String> = emptyMap(),
    val scryfallId: String? = null,
    val releaseDate: String? = null,
    val rulings: List<Ruling> = emptyList(),
    /**
     * Clockwise rotation in degrees to apply to the card art when rendered (default 0 = none).
     * Used for flip-layout tokens whose only Scryfall image shows the *other* face upright: the
     * Wilds of Eldraine Role tokens are printed two-to-a-card (e.g. "Wicked // Cursed"), so the
     * bottom face ("Cursed") reads upside-down on the single available image and needs 180. Purely
     * cosmetic — the engine never reads this; it flows through to the client renderer only.
     */
    val imageRotation: Int = 0,
    /**
     * Whether this printing is part of the set's draft/sealed product. Mirrors Scryfall's
     * `booster` field — false for Special Guests, The List, promos, and other non-draft slots.
     * Drives [com.wingedsheep.engine.limited.BoosterGenerator]: gates both the booster pool and the
     * basic-land variants a set can offer during draft/sealed deck building (so a basic art variant
     * marked `false` is still defined, just never the printing limited hands out — of the remaining
     * variants, the standard art wins; see [BasicLandArt]).
     */
    val inBooster: Boolean = true
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

    /**
     * Adventurer card (CR 715). The primary characteristics in [CardDefinition] describe the
     * creature face; [CardDefinition.cardFaces] holds the single Adventure face (an instant or
     * sorcery with its own name, mana cost, type line, oracle text, target requirements, and
     * spell effect).
     *
     * Casting flow: from hand the owner may cast either the creature (primary characteristics)
     * or the Adventure (face[0]'s alternative characteristics, signalled by [CastSpell.faceIndex]).
     * When an Adventure resolves, the card is exiled instead of going to the graveyard, and the
     * caster gains permission to cast the creature face from exile (CR 715.4-5).
     */
    ADVENTURE,

    /**
     * Omen card (Tarkir: Dragonstorm). Structurally a sibling of [ADVENTURE]: the primary
     * characteristics describe a permanent (a creature), and [CardDefinition.cardFaces] holds
     * the single Omen face — an instant or sorcery with its own name, mana cost, type line,
     * oracle text, target requirements, and spell effect.
     *
     * Casting flow is identical to [ADVENTURE]: from hand the owner may cast either the permanent
     * (primary characteristics) or the Omen (face[0]'s alternative characteristics, signalled by
     * [CastSpell.faceIndex]). The difference is at resolution — when an Omen resolves, instead of
     * exiling itself and granting a cast-from-exile permission, the card is **shuffled into its
     * owner's library** ("then shuffle this card into its owner's library"). From every zone other
     * than the stack (and on the stack when not cast as the Omen) the card has only the permanent's
     * characteristics, exactly like an adventurer.
     */
    OMEN,

    /**
     * Modal double-faced card (CR 712). The front face is described by the primary
     * [CardDefinition] characteristics; [CardDefinition.cardFaces] holds the single back face
     * (its own name, mana cost, type line, oracle text, and — for a spell back — a `spell { }`
     * script). Unlike a transforming DFC, the two faces are unrelated: from hand the owner casts
     * **one** face (front via primary characteristics, back signalled by [CastSpell.faceIndex] = 0)
     * and never both. There is no exile-then-recast linkage like [ADVENTURE]; a spell back resolves
     * as an ordinary spell (going to the graveyard, or to exile when its script sets
     * `selfExileOnResolve`). Faces share one physical card, so each face carries its own art
     * ([CardFace.imageUri]).
     */
    MODAL_DFC,

    /**
     * Preparation card (Secrets of Strixhaven). The primary characteristics in [CardDefinition]
     * describe the creature face; [CardDefinition.cardFaces] holds the single "prepare spell"
     * face — an instant or sorcery with its own name, mana cost, type line, oracle text, target
     * requirements, and spell effect.
     *
     * Unlike [ADVENTURE], a preparation card is only ever cast with its **creature**
     * characteristics (its prepare spell is never cast from hand). It enters with the
     * [com.wingedsheep.sdk.core.Keyword.PREPARED] keyword, which says "This creature enters
     * prepared."
     *
     * Becoming prepared (per the official rulings): the controller creates a copy of the prepare
     * spell (`cardFaces[0]`) in exile. That copy persists in exile while the permanent is on the
     * battlefield and prepared, and the controller may cast that copy from exile (paying its
     * mana cost). Casting the copy makes the creature stop being prepared. If the permanent
     * leaves the battlefield or otherwise stops being prepared, the exiled copy ceases to exist.
     */
    PREPARE,
}

/**
 * One face of a multi-face card.
 *
 * For [CardLayout.SPLIT] cards, every face carries its own name, mana cost, type line, oracle text,
 * keywords, and per-face script. The engine reads abilities from a face's script when that face's
 * door / mode is "active" for the permanent (see CR 709.5 for Rooms, where locked halves are
 * suppressed).
 *
 * A face's [script] may carry a `spellEffect` (with `targetRequirements`) for instant/sorcery
 * halves — the classic Invasion split cards (Pain // Suffering, Stand // Deliver, Wax // Wane) and
 * Adventure faces use this. Permanent halves (Rooms) instead carry triggered / activated / static
 * abilities. Aura targets and replacement effects on a face are not modeled yet.
 */
@Serializable
data class CardFace(
    val name: String,
    val manaCost: ManaCost,
    val typeLine: TypeLine,
    val oracleText: String = "",
    val keywords: Set<Keyword> = emptySet(),
    val script: CardScript = CardScript.EMPTY,
    /**
     * Art for this face, when it differs from the primary characteristics' art. Only meaningful
     * for layouts whose faces are printed on separate sides — i.e. [CardLayout.MODAL_DFC], where
     * the back face has its own Scryfall image. SPLIT/ADVENTURE faces share the front art and
     * leave this null.
     */
    val imageUri: String? = null,
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
    /**
     * The number printed in a battle's lower right corner (CR 310.4a). A battle has the intrinsic
     * ability "this permanent enters with this many defense counters on it" (CR 310.4b), so on the
     * battlefield its defense is its defense-counter count (CR 310.4c) rather than this field. The
     * battle analogue of [startingLoyalty]; null for every other card type.
     */
    val startingDefense: Int? = null,
    val legalFormats: Set<DeckFormat> = emptySet(),  // Formats in which the card is legal (Scryfall-sourced)
    val colorIdentityOverride: Set<Color>? = null,  // Authoritative Scryfall color identity; null = derive from heuristic
    val colorIndicator: Set<Color>? = null,  // Explicit color indicator (CR 204); null = no indicator, color comes from mana cost
    val layout: CardLayout = CardLayout.NORMAL,
    val cardFaces: List<CardFace> = emptyList(),  // Populated for non-NORMAL layouts (e.g. SPLIT Rooms)
    /**
     * True when the card is printed with genuinely no mana cost — not even "{0}" — so it can
     * never be cast normally (CR 202.1b: having no mana cost represents an unpayable cost;
     * CR 118.6: attempting to pay an unpayable cost is illegal), only via an alternative cost or
     * a "play without paying its mana cost" effect (e.g. Ancestral Vision's Suspend). Set by the
     * `card { }` DSL exactly when its `manaCost` string is blank; distinct from [manaCost] being
     * [ManaCost.ZERO] for other reasons (a printed "{0}" cost parses to a non-empty one-symbol
     * list and leaves this false).
     */
    val hasNoManaCost: Boolean = false,
    /**
     * True for a **meld result** (CR 701.42) — the single permanent two meld cards combine into,
     * such as Chittering Host (Graf Rats + Midnight Scavengers) or Brisela, Voice of Nightmares
     * (Bruna, the Fading Light + Gisela, the Broken Blade).
     *
     * A meld result is physically the *back halves* of its two meld parts, not a card of its own:
     * it is never opened in a booster, never drafted, never picked in limited, and never put in a
     * deck — the only way it exists is by melding the pair on the battlefield. We nonetheless
     * define it as a standalone [CardDefinition] so the corpus carries its characteristics for when
     * meld is supported (today the parts' meld triggers are deliberately unwired).
     *
     * That "in the corpus but not a real deck card" split is exactly what this flag exists for:
     * every pool that answers *"which cards can a player end up owning?"* — booster / draft /
     * sealed pools, constructed pools, the deckbuilder catalog — filters it out. Scryfall can't be
     * the source here: it marks meld results `booster: true` and format-legal, because the physical
     * card they're printed on is in the booster and legal (as the meld *parts*).
     */
    val meldResult: Boolean = false,
) {
    init {
        if (typeLine.isCreature) {
            requireNotNull(creatureStats) { "Creature cards must have power/toughness: $name" }
        }
    }

    val cmc: Int get() = manaCost.cmc

    /**
     * The object's colors (CR 202.2): the colors of the mana-cost symbols (202.2c) *plus* the
     * colors of any explicit [colorIndicator] (202.2e / 204.2) — the two combine, they don't
     * override. A card with an empty mana cost and a black color indicator is exactly black; a
     * normal card (no indicator) is just its mana-cost colors. Used e.g. by The Grim Captain, whose
     * transformed back face has no mana cost and a black color indicator.
     */
    val colors: Set<Color>
        get() = if (colorIndicator == null) manaCost.colors else manaCost.colors + colorIndicator

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
     * An explicit [colorIndicator] (rule 204) contributes its colors here too (CR 903.4), so a
     * transformed back face like The Grim Captain's — no mana cost, black color indicator — has
     * black in its color identity.
     *
     * If [colorIdentityOverride] is set (typically from the Scryfall bulk-data sync), that value
     * is authoritative and the heuristic is skipped. Use the override for cards where the
     * heuristic differs from Scryfall (color indicators, devoid, hybrid quirks, etc.).
     */
    val colorIdentity: Set<Color>
        get() {
            colorIdentityOverride?.let { return it }
            val identity = manaCost.colors.toMutableSet()
            colorIndicator?.let(identity::addAll)
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

    /**
     * The `PrintingRef` for this definition's *default* printing — derived from [setCode] and
     * [ScryfallMetadata.collectorNumber]. Returns null if either is missing (e.g. test cards
     * authored without Scryfall metadata). Used by the deckbuilder catalog to default to a
     * concrete printing when the user hasn't picked one explicitly.
     */
    val defaultPrintingRef: PrintingRef?
        get() {
            val set = setCode ?: return null
            val cn = metadata.collectorNumber ?: return null
            return PrintingRef(set, cn)
        }

    /**
     * Return a copy of this definition presenting a different [printing] — the card's oracle
     * identity (script, types, P/T, name) is untouched; only the presentation metadata that the
     * client renders (set code, collector number, art, artist, Scryfall id) is overlaid from the
     * printing. The back-face art is overlaid too, but only when this card actually has a
     * [backFace] and the printing carries a back-face image (so a stray back image on a
     * single-faced printing is ignored).
     *
     * This is the seam the booster generator uses to make a generated card show its showcase /
     * borderless art, and is equally usable wherever a chosen [PrintingRef] should re-skin a
     * card for display.
     */
    fun withPrinting(printing: Printing): CardDefinition = copy(
        setCode = printing.setCode,
        metadata = metadata.copy(
            collectorNumber = printing.collectorNumber,
            artist = printing.artist ?: metadata.artist,
            imageUri = printing.imageUri ?: metadata.imageUri,
            scryfallId = printing.scryfallId ?: metadata.scryfallId,
            releaseDate = printing.releaseDate ?: metadata.releaseDate,
        ),
        backFace = backFace?.let { back ->
            val backImage = printing.backFaceImageUri ?: return@let back
            back.copy(metadata = back.metadata.copy(imageUri = backImage))
        },
    )

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
    val isAdventure: Boolean get() = layout == CardLayout.ADVENTURE
    val isOmen: Boolean get() = layout == CardLayout.OMEN
    val isPreparation: Boolean get() = layout == CardLayout.PREPARE

    /**
     * True if this card is a Room (CR 709.5 + 205.3h). A Room is a split-layout enchantment whose
     * shared type line carries the `Room` subtype (the type line is shared across faces per
     * 709.5a). Drives the engine's Door / unlock logic and the client's split-card rendering.
     */
    val isRoom: Boolean
        get() = isSplit && cardFaces.any { it.typeLine.isRoom }
    /**
     * True if this face is **printed sideways** — its card image is an ordinary portrait file
     * containing a card lying on its side, so every surface that draws it has to rotate it 90° and
     * give it a landscape footprint.
     *
     * The single place that decides what "landscape" means. Two families qualify today: split
     * layouts (CR 709 — Pain // Suffering, and Rooms) and battles (CR 310). A future landscape card
     * type is one clause here and nothing else: the game view (`ClientCard.isLandscapeFace`), the
     * sealed/draft view (`SealedCardInfo.isLandscape`), and every renderer downstream read this
     * rather than re-deriving orientation from layouts and type lines.
     *
     * A property of the *face*, not the card — a transforming double-faced card can be landscape on
     * one side and portrait on the other (Invasion of Innistrad // Deluge of the Dead).
     */
    val isLandscapePrint: Boolean get() = isSplit || isBattle
    val isPlaneswalker: Boolean get() = CardType.PLANESWALKER in typeLine.cardTypes
    val isBattle: Boolean get() = typeLine.isBattle
    val isSiege: Boolean get() = typeLine.isSiege
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

    /**
     * True if this card has at least one intrinsic activated ability that isn't a mana
     * ability and isn't a loyalty ability, activatable from the battlefield. Used by static
     * filters like Tsabo's Web ("each land with an activated ability that isn't a mana ability").
     */
    val hasNonManaActivatedAbility: Boolean
        get() = script.activatedAbilities.any {
            !it.isManaAbility &&
                !it.isPlaneswalkerAbility &&
                it.activateFromZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD
        }

    /**
     * True if this card has at least one intrinsic activated ability (of any kind — mana,
     * loyalty, or otherwise) activatable from the battlefield or the graveyard. Unlike
     * [hasNonManaActivatedAbility] this counts mana abilities, because "a permanent/card with an
     * activated ability" (e.g. the craft material clause on The Enigma Jewel — "four or more nonlands
     * with activated abilities") is satisfied by a mana rock or mana dork just as much as by a tapper.
     * Reflects printed abilities only; abilities granted by other continuous effects are not counted.
     *
     * Scoped to abilities that function from the **battlefield or the graveyard** — the two zones a
     * craft material can be drawn from ("permanents you control and/or cards in your graveyard", CR
     * 702.167a). So a graveyard card whose only activated ability is graveyard-activated (e.g. Undead
     * Gladiator's `{1}, Discard a card: Return this from your graveyard …`) still counts as "with an
     * activated ability." A hand-only ability (cycling) does not, since it never functions on an
     * object that is being used as material.
     */
    val hasActivatedAbility: Boolean
        get() = script.activatedAbilities.any {
            it.activateFromZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD ||
                it.activateFromZone == com.wingedsheep.sdk.core.Zone.GRAVEYARD
        }

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
         * Creates a **modal** double-faced card whose back face is a permanent (CR 712.3), for the
         * Marvel Super Heroes hero cycle: Jennifer Walters // The Sensational She-Hulk, Bruce
         * Banner // The Incredible Hulk, and the rest.
         *
         * These are modal *and* transforming, which CR 712.3 explicitly allows: *"Modal
         * double-faced cards have a Magic card face on each side. These faces are usually
         * independent from one another, **but they may have an ability that allows them to
         * 'transform' or 'convert' on either face**."* So the card supports two routes to the back
         * face, and both must work:
         *
         *  - **Cast it.** Per CR 712.11b the caster chooses a face before putting the card on the
         *    stack, and per CR 712.11c only that face is evaluated — so the back face is castable
         *    from hand for **its own printed mana cost**, and resolves onto the battlefield back
         *    face up (CR 712.13).
         *  - **Transform into it.** The front face's printed activated ability
         *    (`{3}{G}{W}{W}: Transform Jennifer Walters`) flips a front-face-up permanent, exactly
         *    as it would on a nonmodal DFC.
         *
         * Both routes reach the same [backFace] definition, which is why the back is a full
         * [CardDefinition] here rather than a [CardFace]: it needs P/T, keywords and its own
         * abilities on the battlefield. (Modal DFCs with a *spell* back — Flamescroll Celebrant //
         * Revel in Silence — use `modalBack { }` in the DSL and live in [cardFaces] instead; a
         * spell face never becomes a permanent and needs none of that.)
         *
         * **The back face keeps its printed mana cost and takes no color indicator.** That is not
         * cosmetic. CR 712.8f — *"While a modal double-faced spell is on the stack or a modal
         * double-faced permanent is on the battlefield, it has only the characteristics of the face
         * that's up"* — has no mana-value exception, unlike CR 712.8e for nonmodal DFCs. So a
         * transformed The Sensational She-Hulk has mana value 6, not Jennifer Walters' 2, and its
         * G/W colors come from `{3}{G}{W}{W}` rather than from a CR 204 color indicator (which is
         * why the printed card shows none).
         *
         * @param frontFace The front face (must be a permanent).
         * @param backFace The back face (must be a permanent, and must carry its own mana cost —
         *   it is castable).
         */
        fun modalDoubleFacedPermanent(
            frontFace: CardDefinition,
            backFace: CardDefinition
        ): CardDefinition {
            require(frontFace.isPermanent) { "Front face must be a permanent: ${frontFace.name}" }
            require(backFace.isPermanent) { "Back face must be a permanent: ${backFace.name}" }
            require(!backFace.manaCost.isEmpty()) {
                "Modal DFC back face '${backFace.name}' must have its own mana cost — it is castable (CR 712.11b)"
            }
            require(backFace.colorIndicator == null) {
                "Modal DFC back face '${backFace.name}' takes no color indicator; its colors come " +
                    "from its own mana cost (CR 712.8f)"
            }
            return frontFace.copy(backFace = backFace, layout = CardLayout.MODAL_DFC)
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
