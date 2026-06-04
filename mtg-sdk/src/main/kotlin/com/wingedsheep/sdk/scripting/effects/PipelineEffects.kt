package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Pipeline Vocabulary Types
// =============================================================================

/**
 * Where to gather cards from.
 */
@Serializable
sealed interface CardSource {
    val description: String

    /**
     * Top N cards of a player's library.
     */
    @SerialName("TopOfLibrary")
    @Serializable
    data class TopOfLibrary(
        val count: DynamicAmount,
        val player: Player = Player.You
    ) : CardSource {
        override val description: String = "the top ${count.description} cards of ${player.possessive} library"
    }

    /**
     * Cards from a specific zone matching an optional filter.
     *
     * [player] may be a multi-player reference ([Player.Each], [Player.ActivePlayerFirst],
     * [Player.EachOpponent]) to gather from every relevant player's copy of the zone at once
     * (e.g. "all creature cards in each player's graveyard"). Single-player references gather
     * from just that player's zone.
     */
    @SerialName("FromZone")
    @Serializable
    data class FromZone(
        val zone: Zone,
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : CardSource {
        override val description: String = "${filter.description} cards in ${player.possessive} ${zone.displayName}"
    }

    /**
     * Cards from multiple zones matching an optional filter.
     * Used for "search your graveyard, hand, and/or library" effects.
     *
     * Like [FromZone], [player] may be a multi-player reference ([Player.Each],
     * [Player.ActivePlayerFirst], [Player.EachOpponent]) to gather the listed zones across
     * every relevant player at once.
     */
    @SerialName("FromMultipleZones")
    @Serializable
    data class FromMultipleZones(
        val zones: List<Zone>,
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : CardSource {
        override val description: String = "${filter.description} cards in ${player.possessive} ${zones.joinToString(", ") { it.displayName }}"
    }

    /**
     * Read cards from a previously stored collection variable.
     */
    @SerialName("FromVariable")
    @Serializable
    data class FromVariable(val variableName: String) : CardSource {
        override val description: String = "the $variableName cards"
    }

    /**
     * Permanents controlled by a player (uses projected state to respect control-changing effects).
     */
    @SerialName("ControlledPermanents")
    @Serializable
    data class ControlledPermanents(
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : CardSource {
        override val description: String = "permanents ${player.possessive} control"
    }

    /**
     * All permanents on the battlefield matching a filter (any controller by default).
     * Uses projected state for type/color/keyword checks to correctly handle continuous effects.
     *
     * When [player] is [Player.Each], gathers from all players' battlefields.
     * When [player] is a specific player, gathers only permanents that player controls (projected).
     */
    @SerialName("BattlefieldMatching")
    @Serializable
    data class BattlefieldMatching(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val player: Player = Player.Each,
        val excludeSelf: Boolean = false,
        val includeAttachments: Boolean = false,
        /**
         * Exclude the triggering entity from the gathered set. Used by "all *other*
         * creatures … with it" triggered effects (Spreading Plague), where "it" is the
         * triggering creature, which itself matches a "shares a [property] with it" filter.
         */
        val excludeTriggering: Boolean = false
    ) : CardSource {
        override val description: String = buildString {
            if (excludeSelf || excludeTriggering) append("all other ") else append("all ")
            append("${filter.description} permanents on the battlefield")
            if (includeAttachments) append(" and all permanents attached to them")
            if (player != Player.Each) {
                append(" ${player.possessive} control")
            }
        }
    }

    /**
     * Permanents that were tapped as part of the ability's activation cost.
     * Reads from `EffectContext.tappedPermanents`, populated at cost-payment time.
     *
     * Use with [GatherSubtypesEffect] to extract the tapped creatures' subtypes
     * for filtering (e.g., Cryptic Gateway).
     */
    @SerialName("TappedAsCost")
    @Serializable
    data object TappedAsCost : CardSource {
        override val description: String = "creatures tapped this way"
    }

    /**
     * Cards from the source permanent's linked exile (LinkedExileComponent).
     * Returns the entity IDs stored in the component, filtered to only those
     * currently in exile.
     *
     * @property count When set, only gather the first [count] cards from the ordered pile.
     *   When null (default), gather all linked exile cards.
     */
    /**
     * The spell or ability's resolved targets, taken from [EffectContext.targets].
     *
     * Yields the entity ids of every [ChosenTarget] whose underlying object is a
     * permanent / card / spell on the stack (Player targets are skipped — they aren't
     * cards). The cards are NOT moved or modified; the gather only references them so
     * subsequent pipeline steps (Capture, Move, …) can operate on the set.
     *
     * Pair with [CaptureControllersEffect] before any move-out-of-battlefield step
     * when downstream logic needs to know who controlled each card before it left,
     * since `ControllerComponent` is stripped on the way to the graveyard.
     */
    @SerialName("ChosenTargets")
    @Serializable
    data object ChosenTargets : CardSource {
        override val description: String = "the targets"
    }

    @SerialName("FromLinkedExile")
    @Serializable
    data class FromLinkedExile(
        val count: Int? = null
    ) : CardSource {
        override val description: String = buildString {
            if (count != null) append("the top $count of ") else append("")
            append("cards exiled by this permanent")
        }
    }

    /**
     * The ability's own source card ([com.wingedsheep.sdk.scripting.references] sourceId).
     *
     * Yields a single-element collection referencing the source, regardless of which zone
     * it currently occupies. Lets a self-referential ability feed its own card into a
     * pipeline that expects a collection — e.g. the suspend countdown gathering the exiled
     * card so [CastFromCollectionWithoutPayingCostEffect] can play it.
     */
    @SerialName("Self")
    @Serializable
    data object Self : CardSource {
        override val description: String = "this card"
    }
}

/**
 * Where to move cards to.
 */
@Serializable
sealed interface CardDestination {
    val description: String

    /**
     * Move cards to a specific zone.
     */
    @SerialName("ToZone")
    @Serializable
    data class ToZone(
        val zone: Zone,
        val player: Player = Player.You,
        val placement: ZonePlacement = ZonePlacement.Default
    ) : CardDestination {
        override val description: String = buildString {
            append(zone.displayName)
            if (placement != ZonePlacement.Default) {
                append(" (${placement.name.lowercase()})")
            }
        }
    }
}

/**
 * How cards are selected from a collection.
 */
@Serializable
sealed interface SelectionMode {
    val description: String

    /**
     * Player must choose exactly N cards.
     */
    @SerialName("ChooseExactly")
    @Serializable
    data class ChooseExactly(val count: DynamicAmount) : SelectionMode {
        override val description: String = "choose exactly ${count.description}"
    }

    /**
     * Player may choose up to N cards.
     */
    @SerialName("ChooseUpTo")
    @Serializable
    data class ChooseUpTo(val count: DynamicAmount) : SelectionMode {
        override val description: String = "choose up to ${count.description}"
    }

    /**
     * Select all cards (no choice needed).
     */
    @SerialName("SelectAll")
    @Serializable
    data object All : SelectionMode {
        override val description: String = "all"
    }

    /**
     * Randomly select N cards (no player choice — engine picks randomly).
     * Used for "discard X cards at random" effects.
     */
    @SerialName("Random")
    @Serializable
    data class Random(val count: DynamicAmount) : SelectionMode {
        override val description: String = "random ${count.description}"
    }

    /**
     * Player may select any number of cards (0 to the full collection size).
     * Used for "put any number of cards" effects like Tempting Wurm.
     */
    @SerialName("ChooseAnyNumber")
    @Serializable
    data object ChooseAnyNumber : SelectionMode {
        override val description: String = "any number"
    }
}

/**
 * Additional constraint on a selection beyond what [SelectionMode] expresses.
 *
 * Restrictions compose with the base selection mode: the mode sets the bounds
 * (e.g., "up to 9") and restrictions narrow what combinations are valid
 * (e.g., "at most one of each card type"). The executor uses restrictions to
 * tighten the selection's maximum and normalize the player's response on
 * resolution so no invalid combination is accepted.
 *
 * This is a sealed interface so new restrictions (e.g., "distinct mana values",
 * "no two cards of the same color") can be added without touching the mode.
 */
@Serializable
sealed interface SelectionRestriction {
    val description: String

    /**
     * At most one card of each card type may be selected. The natural upper
     * bound is the number of distinct card types present in the source
     * collection (nine in practice — artifact, battle, creature, enchantment,
     * instant, kindred, land, planeswalker, sorcery). Used for "for each card
     * type, you may ..." effects like Portent of Calamity.
     *
     * The executor enforces the constraint server-side: if the player's
     * response names multiple cards sharing a card type, only the first such
     * card (in response order) is kept for each type.
     */
    @SerialName("OnePerCardType")
    @Serializable
    data object OnePerCardType : SelectionRestriction {
        override val description: String = "at most one card of each card type"
    }

    /**
     * At most one card of each colour may be selected. Colourless cards do not
     * consume any colour and are always selectable (subject to the base
     * [SelectionMode] cap).
     *
     * When [matchControllerPermanentColors] is true, eligibility is also
     * narrowed to cards whose colours intersect the colours of permanents the
     * chooser controls — this captures the "for each of those colours [the
     * colours among permanents you control], you may exile a card of that
     * colour" wording used by Lorwyn Eclipsed's Vivid mechanic (Sanar,
     * Innovative First-Year). With this flag, colourless cards are never
     * eligible (they share no colour with the controller's permanents).
     *
     * The executor enforces this server-side: if the player's response names
     * multiple cards sharing a colour, only the first such card (in response
     * order) is kept per colour.
     */
    @SerialName("OnePerColor")
    @Serializable
    data class OnePerColor(
        val matchControllerPermanentColors: Boolean = false
    ) : SelectionRestriction {
        override val description: String = buildString {
            append("at most one card of each colour")
            if (matchControllerPermanentColors) {
                append(", and only colours of permanents you control")
            }
        }
    }

    /**
     * At most one card of each name may be selected. Cards with the same name
     * are treated as interchangeable; only the first such card (in response order)
     * is kept.
     *
     * Used for "creature cards with different names" effects like Behold the Sinister Six!.
     */
    @SerialName("OnePerCardName")
    @Serializable
    data object OnePerCardName : SelectionRestriction {
        override val description: String = "at most one card of each name"
    }

    /**
     * The sum of selected cards' mana values must not exceed [max]. {X} contributes 0
     * (per CR 202.3e for cards not on the stack). Used for "with total mana value N or
     * less" wording like Scout for Survivors.
     *
     * The executor enforces this server-side: any selection whose total exceeds [max]
     * has cards trimmed (in response order) until the cap is met.
     */
    @SerialName("TotalManaValueAtMost")
    @Serializable
    data class TotalManaValueAtMost(val max: Int) : SelectionRestriction {
        override val description: String = "with total mana value $max or less"
    }

    /**
     * At most one land of each basic land type (Plains/Island/Swamp/Mountain/Forest)
     * may be selected. A selected land claims *every* basic land type it has, so a dual
     * land that is both a Plains and an Island consumes both slots (Global Ruin ruling).
     *
     * Unlike [OnePerColor] — where a colourless card is unconstrained and always
     * selectable — a land with **no** basic land type can't be kept at all: "chooses
     * ... a land of each basic land type" only lets you keep typed lands, so a typeless
     * land falls into the remainder. The executor enforces this server-side: a selection
     * naming a typeless land, or a second land sharing an already-claimed basic type, is
     * rejected (in response order) and falls through to the remainder collection.
     *
     * Used for Global Ruin.
     */
    @SerialName("OnePerBasicLandType")
    @Serializable
    data object OnePerBasicLandType : SelectionRestriction {
        override val description: String = "at most one land of each basic land type"
    }
}

/**
 * Who makes the selection decision.
 */
@Serializable
enum class Chooser {
    /** The controller of the spell/ability decides */
    Controller,
    /** An opponent decides */
    Opponent,
    /** The target player decides (resolved from context.targets[0]) */
    TargetPlayer,
    /** The triggering player decides (resolved from trigger context) */
    TriggeringPlayer,
    /**
     * The controller of the source spell/ability decides, regardless of any per-iteration
     * controller swap (e.g., inside [ForEachPlayerEffect] which sets the iterated player as
     * the current controller). Resolves through `context.sourceId` -> projected controller.
     * Use this for cards like Winnowing where the spell's caster makes a choice for each
     * player during a per-player iteration.
     */
    SourceController,
    /**
     * The controller of the cards being selected from (the `from` collection) decides.
     * When every gathered card shares a single controller — e.g. "two target creatures
     * controlled by the same player" — this resolves to that player, who may be the
     * spell's controller or an opponent. Resolves through the projected controller of the
     * first card in the source collection. Used by Barrin's Spite ("their controller
     * chooses and sacrifices one of them").
     */
    ControllerOfSelection,

    /**
     * The controller of the targeted permanent (`context.targets[0]`) decides — distinct
     * from [TargetPlayer], which treats the target itself as the deciding player. Resolves
     * through the target's `ControllerComponent`, falling back to its owner when the
     * permanent has already left the battlefield (e.g. it was destroyed earlier in the same
     * resolution). Used by "destroy target permanent. Its controller searches their
     * library…"-style effects (Magmatic Hellkite), where the destroyed permanent's
     * controller performs a follow-up search/choice.
     */
    ControllerOfTarget
}

/**
 * How to order cards when moving a collection.
 */
@Serializable
enum class CardOrder {
    /** Controller chooses the order (prompts if > 1 card) */
    ControllerChooses,
    /** Random order */
    Random,
    /** Preserve the existing order */
    Preserve
}

// =============================================================================
// Pipeline Effect Types
// =============================================================================

/**
 * Gather cards from a source and store them in a named collection.
 *
 * This is the first step in a pipeline: it reads cards from a source (library top,
 * zone, or variable) and stores them for subsequent Select/Move steps.
 *
 * @property source Where to get the cards from
 * @property storeAs Name of the collection to store the cards in
 * @property revealed Whether the gathered cards are revealed to all players
 */
@SerialName("GatherCards")
@Serializable
data class GatherCardsEffect(
    val source: CardSource,
    val storeAs: String,
    val revealed: Boolean = false
) : Effect {
    override val description: String = buildString {
        if (revealed) append("Reveal ") else append("Look at ")
        append(source.description)
    }
}

/**
 * Snapshot the current controller of each entity in a stored collection, writing the
 * controller ids to `storedCollections[storeAs]` as a parallel list (entry _i_ in
 * `storeAs` is the controller of entry _i_ in `from`).
 *
 * Lives in the pipeline because `ControllerComponent` is stripped when a permanent
 * leaves the battlefield, so any "this way"-style downstream step (Builder's Bane:
 * "deals damage to each player equal to the number of artifacts they controlled that
 * were put into a graveyard this way") must capture the controller before the move.
 *
 * Pair with [CardSource.ChosenTargets] → [MoveCollectionEffect] (Destroy) → a
 * controller-aware consumer like [ForEachCapturedControllerEffect].
 *
 * Entries whose entity has no `ControllerComponent` (already off the battlefield)
 * fall back to the card's owner; permanents always have an owner, so the captured
 * list is the same length as [from] and indices line up 1:1.
 *
 * @property from Stored collection whose entities' controllers to capture.
 * @property storeAs Stored collection key under which the parallel controller list lives.
 */
@SerialName("CaptureControllers")
@Serializable
data class CaptureControllersEffect(
    val from: String,
    val storeAs: String
) : Effect {
    override val description: String = "Capture the controllers of the $from cards"
}

/**
 * Iterate the unique controllers captured by [CaptureControllersEffect] for cards that
 * appear in a follow-on collection (e.g. cards that actually died after a destroy),
 * running [effects] once per controller with:
 *
 *   - `context.controllerId` set to the iterating controller, so `Player.You` and
 *     `EffectTarget.Controller` resolve to that player inside the sub-effects.
 *   - `context.pipeline.storedNumbers[countVariable]` set to the tally for that
 *     controller, so sub-effects can scale via `DynamicAmount.VariableReference`.
 *   - The outer pipeline collections preserved (unlike [ForEachPlayerEffect], which
 *     deliberately wipes them) so sub-effects can still reference the dead pile,
 *     the captured snapshot, etc.
 *
 * Cross-references three pipeline lists:
 *   - [collection]: cards that survived the move-out step (e.g. `storeMovedAs` output).
 *     Only these contribute to per-controller tallies.
 *   - [originalCollection]: the gather-time list; the index space for [controllerSnapshot].
 *   - [controllerSnapshot]: parallel controller-id list aligned with [originalCollection].
 *
 * Order: iterates in turn order, starting from the active player, so deterministic
 * sequencing of any sub-effects that mutate shared state (life loss, damage, draws).
 *
 * General enough for any "for each player, do X scaled by how many of their permanents
 * went this way" payoff — Builder's Bane (deal damage), `lose life`, `discard a card
 * for each`, `mill`, etc.
 *
 * @property collection Post-action collection (e.g. "destroyed" cards) that gates the tally.
 * @property originalCollection Pre-action collection; defines the index space.
 * @property controllerSnapshot Parallel controller-id list (same length as [originalCollection]).
 * @property countVariable Variable name under which per-iteration count is stored in
 *   `storedNumbers`. Defaults to `"iterationCount"`.
 * @property effects Sub-effects executed once per controller.
 */
@SerialName("ForEachCapturedController")
@Serializable
data class ForEachCapturedControllerEffect(
    val collection: String,
    val originalCollection: String,
    val controllerSnapshot: String,
    val countVariable: String = "iterationCount",
    val effects: List<Effect>
) : Effect {
    override val description: String = buildString {
        append("For each player who controlled at least one of the $collection cards, ")
        append(effects.joinToString(". ") { it.description.replaceFirstChar { c -> c.lowercase() } })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var changed = false
        val newEffects = effects.map { val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n }
        return if (changed) copy(effects = newEffects) else this
    }
}

/**
 * Extract the subtypes of each entity in a stored collection and save the result
 * as a list of subtype sets in `storedSubtypeGroups[storeAs]`.
 *
 * Pair with [CardPredicate.HasSubtypeInEachStoredGroup] to implement "shares a
 * creature type with each of" semantics. Example pipeline (Cryptic Gateway):
 *
 * ```
 * GatherCards(TappedAsCost, storeAs = "tapped")          // → storedCollections
 * GatherSubtypes(from = "tapped", storeAs = "tappedTypes") // → storedSubtypeGroups
 * GatherCards(Hand, Creature.withSubtypeInEachStoredGroup("tappedTypes"), ...)
 * ```
 *
 * @property from Name of the stored collection whose entities' subtypes to extract
 * @property storeAs Key under which the `List<Set<String>>` is stored
 */
@SerialName("GatherSubtypes")
@Serializable
data class GatherSubtypesEffect(
    val from: String,
    val storeAs: String
) : Effect {
    override val description: String = "gather subtypes of the $from entities"
}

/**
 * Walk a player's library top-down until [count] cards matching [filter] have been
 * revealed (or the library runs out). Stores the matches (0..count cards) and all
 * revealed cards (including any matches) as named collections.
 *
 * Does **not** emit a reveal event — pair with [RevealCollectionEffect] for that.
 *
 * @property player Whose library to walk
 * @property filter The predicate that counts toward stopping when matched
 * @property storeMatch Collection name for the matching cards (empty if no matches found)
 * @property storeRevealed Collection name for ALL cards seen (including any matches)
 * @property count How many matches to gather before stopping (defaults to 1)
 */
@SerialName("GatherUntilMatch")
@Serializable
data class GatherUntilMatchEffect(
    val player: Player = Player.You,
    val filter: GameObjectFilter,
    val storeMatch: String,
    val storeRevealed: String,
    val count: DynamicAmount = DynamicAmount.Fixed(1)
) : Effect {
    override val description: String = buildString {
        append("Reveal cards from the top of ")
        append(player.possessive)
        append(" library until you reveal ${count.description} ${filter.description} card(s)")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newCount = count.applyTextReplacement(replacer)
        return if (newFilter !== filter || newCount !== count) copy(filter = newFilter, count = newCount) else this
    }
}

/**
 * Emit a [com.wingedsheep.engine.core.CardsRevealedEvent] for all cards in the
 * named stored collection. Does not move or change any cards.
 *
 * @property from Name of the stored collection to reveal
 * @property revealToSelf When false, the revealing player does not see the reveal
 *   overlay (they already know — e.g., they just chose the card themselves while
 *   searching their library). Defaults to true so opponents and bystanders still
 *   get the reveal.
 * @property fromZone Optional source zone the revealed cards came from. When both
 *   [fromZone] and [toZone] are set, the client treats this as a zone-transition
 *   reveal and shows every revealed card in the overlay — even ones that landed on
 *   the battlefield in the same update (e.g., Polymorph's matching creature).
 *   Without this, public-info cards would be hidden from the overlay and shown only
 *   as a behold-pulse on the permanent.
 * @property toZone Optional destination zone for the reveal context. See [fromZone].
 */
@SerialName("RevealCollection")
@Serializable
data class RevealCollectionEffect(
    val from: String,
    val revealToSelf: Boolean = true,
    val fromZone: com.wingedsheep.sdk.core.Zone? = null,
    val toZone: com.wingedsheep.sdk.core.Zone? = null
) : Effect {
    override val description: String = "Reveal the $from cards"
}

/**
 * Select cards from a named collection, splitting into selected and remainder.
 *
 * This is the middle step in a pipeline: it presents a choice to the player
 * (or auto-selects) and splits the collection into two named groups.
 *
 * @property from Name of the collection to select from
 * @property selection How many cards are selected (exactly N, up to N, all)
 * @property chooser Who makes the selection (controller or opponent)
 * @property filter Optional filter for which cards can be selected
 * @property storeSelected Name to store the selected cards under
 * @property storeRemainder Name to store the non-selected cards under (null = discard remainder info)
 */
@SerialName("SelectFromCollection")
@Serializable
data class SelectFromCollectionEffect(
    val from: String,
    val selection: SelectionMode,
    val chooser: Chooser = Chooser.Controller,
    val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
    val storeSelected: String,
    val storeRemainder: String? = null,
    /**
     * When true, additionally filter eligible cards to only those with the
     * creature type stored in the effect context's chosenCreatureType.
     * Used for "creature card of the chosen type" patterns.
     */
    val matchChosenCreatureType: Boolean = false,
    /** Custom prompt for the selection decision. If null, a generic prompt is generated. */
    val prompt: String? = null,
    /** Label describing where selected cards go (e.g., "Put on bottom"). Shown in the UI. */
    val selectedLabel: String? = null,
    /** Label describing where non-selected cards go (e.g., "Put on top"). Shown in the UI. */
    val remainderLabel: String? = null,
    /** If true, use the targeting UI (click on battlefield) instead of modal overlay. */
    val useTargetingUI: Boolean = false,
    /**
     * When true, all cards in the collection are shown to the player (even non-eligible ones).
     * Non-eligible cards are displayed but not selectable. Used for "look at" effects like
     * Adventurous Impulse where the player sees all cards but can only choose matching ones.
     */
    val showAllCards: Boolean = false,
    /**
     * Additional constraints applied on top of [selection]. The executor tightens the
     * effective maximum using these and normalizes the player's response so invalid
     * combinations are rejected. Defaults to empty (no extra constraints).
     */
    val restrictions: List<SelectionRestriction> = emptyList(),
    /**
     * When true, always present the selection decision to the chooser even if the
     * eligible set has exactly [selection]'s requested count (i.e., the choice is
     * forced). Used by reveal-then-discard flows where the caster is picking from
     * another player's zone — auto-selecting the one eligible card feels abrupt
     * and players want to see and confirm the pick.
     */
    val alwaysPrompt: Boolean = false
) : Effect {
    override val description: String = buildString {
        if (chooser == Chooser.Opponent) append("An opponent ")
        append(selection.description.replaceFirstChar { it.uppercase() })
        append(" from the $from cards")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Present a binary choice between two named collections (piles) and route them
 * to caller-specified destination collections. Used after a partition step like
 * [SelectFromCollectionEffect] to ask a (typically different) player which
 * pile is which.
 *
 * Concrete example — Fact or Fiction (CR 700.3):
 *  1. Reveal top 5.
 *  2. Opponent partitions them into [pileA] and [pileB] via SelectFromCollection.
 *  3. **Caster** picks which pile is the "keep" pile via ChoosePile — the chosen
 *     pile goes into [storeChosenAs], the other into [storeOtherAs].
 *  4. Subsequent MoveCollection effects route each named collection to its zone.
 *
 * Empty piles are legal — the chooser still gets to assign the empty pile to
 * either side.
 *
 * @property pileA Name of the first pre-existing collection (e.g. "pileA")
 * @property pileB Name of the second pre-existing collection (e.g. "pileB")
 * @property pileALabel Display label for pile A (defaults to "Pile 1")
 * @property pileBLabel Display label for pile B (defaults to "Pile 2")
 * @property chooser Who picks (defaults to the controller of the source spell/ability)
 * @property storeChosenAs Collection name to store the picked pile under
 * @property storeOtherAs Collection name to store the non-picked pile under
 * @property prompt Custom prompt for the decision; defaults to a generic one
 */
@SerialName("ChoosePile")
@Serializable
data class ChoosePileEffect(
    val pileA: String,
    val pileB: String,
    val pileALabel: String = "Pile 1",
    val pileBLabel: String = "Pile 2",
    val chooser: Chooser = Chooser.Controller,
    val storeChosenAs: String,
    val storeOtherAs: String,
    val prompt: String? = null
) : Effect {
    override val description: String =
        "Choose one pile: $pileALabel or $pileBLabel"
}

/**
 * Move all cards in a named collection to a destination zone.
 *
 * This is the final step in a pipeline: it takes a named collection and
 * moves all cards in it to the specified destination.
 *
 * @property from Name of the collection to move
 * @property destination Where to move the cards
 * @property order How to order the cards at the destination
 */
/**
 * How the move should be categorized for event emission.
 */
@Serializable
enum class MoveType {
    /** Standard zone change — emits only ZoneChangeEvent */
    Default,
    /** Discard — also emits CardsDiscardedEvent */
    Discard,
    /**
     * Sacrifice — emits PermanentsSacrificedEvent in addition to ZoneChangeEvent.
     * Cards are routed to their owner's graveyard (not necessarily the destination player),
     * matching MTG rule 701.21a: sacrificed permanents go to their owner's graveyard.
     */
    Sacrifice,
    /**
     * Destroy — respects indestructible (skips those permanents) and regeneration shields.
     * Cards are routed to their owner's graveyard.
     * Destination should be Graveyard; the executor enforces owner routing.
     */
    Destroy
}

@SerialName("MoveCollection")
@Serializable
data class MoveCollectionEffect(
    val from: String,
    val destination: CardDestination,
    val order: CardOrder = CardOrder.Preserve,
    val revealed: Boolean = false,
    /**
     * Whether the reveal overlay is shown to the player who performed the move.
     * Defaults to true. Set to false when the controller chose the cards themselves
     * (they already know what's being "revealed") — only opponents see the overlay.
     * Ignored when [revealed] is false.
     */
    val revealToSelf: Boolean = true,
    val moveType: MoveType = MoveType.Default,
    val linkToSource: Boolean = false,
    val unlinkFromSource: Boolean = false,
    val faceDown: Boolean = false,
    val noRegenerate: Boolean = false,
    val storeMovedAs: String? = null,
    val underOwnersControl: Boolean = false,
    val addCounterType: CounterType? = null,
    /**
     * When true, each card moved to the battlefield is stamped with an
     * `EnteredViaAbilityComponent` referencing the resolving ability's source
     * permanent. Enables "if it wasn't put onto the battlefield with this ability"
     * anti-loop guards (Kodama of the East Tree). Ignored when the destination is
     * not the battlefield.
     */
    val markEnteredViaSourceAbility: Boolean = false
) : Effect {
    override val description: String = buildString {
        if (revealed) append("Reveal and put") else append("Put")
        append(" the $from cards ")
        append(destination.description)
    }
}

/**
 * Reveal cards from the top of a player's library until a card matching
 * the filter is found. The matching card is stored in [storeMatch] and
 * ALL revealed cards (including the match) are stored in [storeRevealed].
 *
 * If no matching card is found (entire library is revealed), [storeMatch] will
 * be empty and [storeRevealed] will contain all revealed cards.
 *
 * Choose a creature type. Stores the chosen type in the effect context
 * for use by subsequent pipeline steps (e.g., GatherUntilMatchEffect with
 * a `withSubtypeFromVariable("chosenCreatureType")` filter).
 *
 * This is a pipeline step that pauses for player input.
 */
@SerialName("ChooseCreatureType")
@Serializable
data object ChooseCreatureTypeEffect : Effect {
    override val description: String = "Choose a creature type"
}

/**
 * The type of option to choose from in a ChooseOptionEffect.
 */
@Serializable
enum class OptionType {
    /** Choose from all creature types */
    CREATURE_TYPE,
    /** Choose from the five Magic colors */
    COLOR,
    /** Choose from the five basic land types (Plains, Island, Swamp, Mountain, Forest) */
    BASIC_LAND_TYPE,
    /**
     * Name a card. Options are every card name the engine knows about (the registry),
     * sorted alphabetically, so the player searches/filters a list rather than typing
     * free text. Pair the stored name with [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameEqualsChosen]
     * to match cards by it. Used by "name a card" effects (Desperate Research, etc.).
     */
    CARD_NAME
}

/**
 * Generic pipeline step that presents a choice from a set of options and stores
 * the result in a named variable (via EffectContext.chosenValues).
 *
 * This is the generic replacement for type-specific choice effects. Downstream
 * pipeline effects can read the chosen value from chosenValues[storeAs].
 *
 * @property optionType What kind of options to present (creature types, colors, etc.)
 * @property storeAs Key under which the chosen value is stored in EffectContext.chosenValues
 * @property prompt Custom prompt text. If null, a default is generated from the option type.
 * @property excludedOptions Options to exclude from the presented list
 */
@SerialName("ChooseOption")
@Serializable
data class ChooseOptionEffect(
    val optionType: OptionType,
    val storeAs: String = "chosenOption",
    val prompt: String? = null,
    val excludedOptions: List<String> = emptyList()
) : Effect {
    override val description: String = buildString {
        append("Choose ")
        append(when (optionType) {
            OptionType.CREATURE_TYPE -> "a creature type"
            OptionType.COLOR -> "a color"
            OptionType.BASIC_LAND_TYPE -> "a basic land type"
            OptionType.CARD_NAME -> "a card name"
        })
    }
}

/**
 * Each player (in APNAP order) chooses a creature type.
 * All chosen types are accumulated and stored as a List<String> under [storeAs]
 * in the effect context's storedStringLists.
 *
 * Used for cards like Harsh Mercy and similar "each player chooses a creature type" effects.
 *
 * @property storeAs Key under which the accumulated list of chosen types is stored
 */
@SerialName("EachPlayerChoosesCreatureType")
@Serializable
data class EachPlayerChoosesCreatureTypeEffect(
    val storeAs: String
) : Effect {
    override val description: String = "Each player chooses a creature type"
}

/**
 * Select a target during effect resolution (mid-pipeline targeting).
 *
 * Unlike cast-time targeting (TargetRequirement on spells/abilities), this step
 * selects targets at resolution time as part of a pipeline. The selected target
 * entity IDs are stored in [storedCollections][storeAs] for use by subsequent
 * pipeline effects via [com.wingedsheep.sdk.scripting.targets.EffectTarget.PipelineTarget].
 *
 * **Important:** This is for non-targeting choices ("choose a creature") or for
 * targets that depend on earlier pipeline results. Do NOT use this for cards whose
 * oracle text says "target" — those must use cast-time targeting so the target is
 * declared when the spell/ability goes on the stack, giving opponents a chance to
 * respond with knowledge of what is being targeted. Mid-resolution selection happens
 * after priority has passed and cannot be responded to.
 *
 * @property requirement The target requirement (reuses all existing TargetRequirement types)
 * @property storeAs Name of the collection to store the selected target IDs in
 */
@SerialName("SelectTarget")
@Serializable
data class SelectTargetEffect(
    val requirement: TargetRequirement,
    val storeAs: String = "pipelineTarget"
) : Effect {
    override val description: String = "Choose ${requirement.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newRequirement = requirement.applyTextReplacement(replacer)
        return if (newRequirement !== requirement) copy(requirement = newRequirement) else this
    }
}

/**
 * Grant "may play from exile" permission to all cards in a named collection.
 * The cards must already be in exile. The engine registers a MayPlayPermission
 * on the game state, allowing the controller to play them as if they were in
 * hand until end of turn (or the configured expiry).
 *
 * Does NOT waive the mana cost — pair with [GrantPlayWithoutPayingCostEffect]
 * to also make them free. Used by impulse-draw effects (Chandra, Act on Impulse, etc.).
 *
 * @property from Name of the collection containing the exiled card(s)
 */
@SerialName("GrantMayPlayFromExile")
@Serializable
data class GrantMayPlayFromExileEffect(
    val from: String,
    val expiry: MayPlayExpiry = MayPlayExpiry.EndOfTurn,
    /**
     * When true, mana of any type may be spent to cast the granted cards. Used by
     * "and mana of any type can be spent to cast that spell" clauses (Taster of Wares,
     * Cruelclaw's Heist).
     */
    val withAnyManaType: Boolean = false,
    /**
     * Optional gate evaluated each time the play permission is checked. Used for cards
     * that grant a conditional may-play, e.g. Possibility Technician's "you may play it
     * if you control a Kavu" — the permission persists with the card in exile, but is
     * only honored while the condition holds. Re-evaluated on every legal-action query.
     */
    val condition: Condition? = null,
    /**
     * When true, a land card played via this permission enters the battlefield tapped.
     * Used by Lightstall Inquisitor-style "each land played this way enters tapped"
     * clauses, where the tapped-on-entry rule is imposed by the granting effect rather
     * than the played card's own script.
     */
    val landEntersTapped: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("${expiry.description.replaceFirstChar { it.uppercase() }}, you may play the $from cards from exile")
        if (condition != null) {
            append(" ")
            append(condition.description)
        }
        if (withAnyManaType) append(", and mana of any type can be spent to cast them")
        if (landEntersTapped) append(". Each land played this way enters tapped")
    }
}

/**
 * Conditionally execute an effect based on the size of a named collection.
 *
 * Used for "if you do" effects where a subsequent action depends on whether
 * a previous pipeline step actually moved/selected any cards. Defaults to
 * "non-empty" semantics ([minSize] = 1) but accepts a higher threshold for
 * "if 4 or more" style checks like Portent of Calamity.
 *
 * @property collection Name of the collection to check
 * @property ifNotEmpty Effect to execute if the collection has at least [minSize] cards
 * @property ifEmpty Effect to execute otherwise (optional)
 * @property minSize Inclusive minimum collection size required to run [ifNotEmpty]
 */
@SerialName("ConditionalOnCollection")
@Serializable
data class ConditionalOnCollectionEffect(
    val collection: String,
    val ifNotEmpty: Effect,
    val ifEmpty: Effect? = null,
    val minSize: Int = 1,
    /** When true, [minSize] is compared against the count of distinct card types across all
     *  cards in the collection rather than the raw collection size. An Artifact Creature in
     *  the collection contributes both Artifact and Creature to the count. */
    val countDistinctCardTypes: Boolean = false,
    /** Restrict the collection to cards matching this filter before measuring against
     *  [minSize]. Used for "if the discarded card is a nonland" style guards. Defaults to
     *  [GameObjectFilter.Any] (no restriction). Ignored when [countDistinctCardTypes] is true. */
    val filter: GameObjectFilter = GameObjectFilter.Companion.Any
) : Effect {
    override val description: String = buildString {
        val restrict = if (filter == GameObjectFilter.Companion.Any) "" else " ${filter.description}"
        if (minSize <= 1) {
            append("If $collection contains a$restrict card, ")
        } else if (countDistinctCardTypes) {
            append("If $collection covers at least $minSize card types, ")
        } else {
            append("If $collection has at least $minSize$restrict cards, ")
        }
        append(ifNotEmpty.description.replaceFirstChar { it.lowercase() })
        if (ifEmpty != null) {
            append(". Otherwise, ")
            append(ifEmpty.description.replaceFirstChar { it.lowercase() })
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newIfNotEmpty = ifNotEmpty.applyTextReplacement(replacer)
        val newIfEmpty = ifEmpty?.applyTextReplacement(replacer)
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newIfNotEmpty !== ifNotEmpty || newIfEmpty !== ifEmpty || newFilter !== filter)
            copy(ifNotEmpty = newIfNotEmpty, ifEmpty = newIfEmpty, filter = newFilter) else this
    }
}

/**
 * Grant "play without paying mana cost" permission to all cards in a named
 * collection until end of turn. Adds a PlayWithoutPayingCostComponent.
 *
 * The card must still be in a playable zone (hand, or exile with
 * [GrantMayPlayFromExileEffect]). Used by Mind's Desire, Cascade, etc.
 *
 * @property from Name of the collection containing the card(s)
 */
@SerialName("GrantPlayWithoutPayingCost")
@Serializable
data class GrantPlayWithoutPayingCostEffect(
    val from: String
) : Effect {
    override val description: String =
        "Until end of turn, you may play the $from cards without paying their mana costs"
}

/**
 * Grant "play with an additional cost" to all cards in a named collection.
 * Used with [GrantMayPlayFromExileEffect] + [GrantPlayWithoutPayingCostEffect]
 * to model "may cast by paying [cost] rather than its mana cost" — the mana is
 * waived by PlayWithoutPayingCost, while this adds a runtime additional cost.
 *
 * Used by The Infamous Cruelclaw ("You may cast that card by discarding a card
 * rather than paying its mana cost").
 *
 * @property from Name of the collection containing the card(s)
 * @property additionalCost The additional cost that must be paid when casting
 */
@SerialName("GrantPlayWithAdditionalCost")
@Serializable
data class GrantPlayWithAdditionalCostEffect(
    val from: String,
    val additionalCost: AdditionalCost
) : Effect {
    override val description: String =
        "Until end of turn, casting the $from cards requires: ${additionalCost.description}"
}

/**
 * Add a generic-mana tax to every card in a named collection so that when the
 * effect controller later casts one of them, the spell costs [amount] more to
 * cast. Stamps PlayWithCostIncreaseComponent on each card in the collection,
 * which CastSpellHandler reads at cast time.
 *
 * Pair with [GrantMayPlayFromExileEffect] to grant a permission *and* tax the
 * cast — used by Lightstall Inquisitor ("Each spell cast this way costs {1}
 * more to cast"), where each opponent exiles a card from their own hand and
 * may play it with this tax.
 *
 * @property from Name of the collection containing the card(s)
 * @property amount Generic mana added to each card's cost when the controller casts it
 */
@SerialName("GrantPlayWithCostIncrease")
@Serializable
data class GrantPlayWithCostIncreaseEffect(
    val from: String,
    val amount: Int
) : Effect {
    override val description: String =
        "Each spell cast from $from costs {$amount} more to cast"
}

/**
 * Grant a single target entity in exile permission to be cast without paying
 * its mana cost. The engine registers a MayPlayPermission and stamps
 * PlayWithoutPayingCostComponent on the target. Optionally marks the spell
 * with ExileAfterResolveComponent so it goes to exile instead of graveyard
 * after resolving or being countered.
 *
 * Unlike the collection-based [GrantMayPlayFromExileEffect] + [GrantPlayWithoutPayingCostEffect],
 * this works on a single targeted entity referenced by [EffectTarget].
 *
 * @property target The entity in exile to grant free cast permission to
 * @property exileAfterResolve If true, the spell will be exiled instead of going to
 *   graveyard after resolution (like Flashback). Used for "exile it instead" clauses.
 */
@SerialName("GrantFreeCastTargetFromExile")
@Serializable
data class GrantFreeCastTargetFromExileEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val exileAfterResolve: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("You may cast ${target.description} without paying its mana cost")
        if (exileAfterResolve) append(". If that spell would be put into a graveyard, exile it instead")
    }
}

// =============================================================================
// Collection Filtering
// =============================================================================

/**
 * How to filter a named collection.
 */
@Serializable
sealed interface CollectionFilter {
    /**
     * Exclude entities that have any subtype matching a stored string list.
     * Reads the list from [storedKey] in the effect context's storedStringLists.
     */
    @SerialName("ExcludeSubtypesFromStored")
    @Serializable
    data class ExcludeSubtypesFromStored(val storedKey: String) : CollectionFilter

    /**
     * Keep only entities that share at least one subtype with the sacrificed creature.
     * Reads subtypes from context.sacrificedPermanents[].subtypes (snapshotted at sacrifice time).
     */
    @SerialName("SharesSubtypeWithSacrificed")
    @Serializable
    data object SharesSubtypeWithSacrificed : CollectionFilter

    /**
     * Keep only entities that match a [GameObjectFilter] using projected state.
     * This is the general-purpose filter for any predicate-based collection filtering.
     */
    @SerialName("MatchesFilter")
    @Serializable
    data class MatchesFilter(val filter: GameObjectFilter) : CollectionFilter

    /**
     * Keep only creatures with the greatest power in the collection.
     * Uses projected state to determine power (respects continuous effects).
     * If multiple creatures are tied for the greatest power, all of them are kept.
     *
     * Used for Crackling Doom and similar "sacrifice the creature with the greatest power" effects.
     */
    @SerialName("GreatestPower")
    @Serializable
    data object GreatestPower : CollectionFilter

    /**
     * Keep only creatures with the least toughness in the collection.
     * Uses projected state to determine toughness (respects continuous effects).
     * If multiple creatures are tied for the least toughness, all of them are kept
     * (so a downstream "exactly one" consumer lets the controller break the tie).
     *
     * Used for the Bolster keyword action (CR 701.36): "Choose a creature with the least
     * toughness among creatures you control and put N +1/+1 counters on it."
     */
    @SerialName("LeastToughness")
    @Serializable
    data object LeastToughness : CollectionFilter

    /**
     * Keep only the cards with the greatest mana value in the collection. If multiple cards are
     * tied for the greatest mana value, all of them are kept (so a downstream "exactly one" consumer
     * can detect a tie).
     *
     * Used by reveal-and-compare effects like Psychic Battle ("the player who revealed the card with
     * the greatest mana value …").
     */
    @SerialName("GreatestManaValue")
    @Serializable
    data object GreatestManaValue : CollectionFilter

    /**
     * Keep only entities whose mana value is at most a dynamic amount.
     * The amount is resolved at execution time from the effect context.
     *
     * Used for "spells with mana value X or less" effects like Villainous Wealth.
     *
     * @property max The maximum mana value (resolved dynamically)
     */
    @SerialName("ManaValueAtMost")
    @Serializable
    data class ManaValueAtMost(val max: DynamicAmount) : CollectionFilter

    /**
     * Keep only entities whose mana value equals a dynamic amount.
     * The amount is resolved at execution time from the effect context.
     *
     * Used for "instant or sorcery with mana value equal to the number of
     * counters on this artifact" effects like Wishing Well.
     *
     * @property value The exact mana value to match (resolved dynamically)
     */
    @SerialName("ManaValueEquals")
    @Serializable
    data class ManaValueEquals(val value: DynamicAmount) : CollectionFilter

    /**
     * Exclude the entity referenced by [entity] from the collection.
     * Used for "another" constraints (e.g., "return another creature card").
     *
     * @property entity The entity reference to exclude
     */
    @SerialName("ExcludeEntity")
    @Serializable
    data class ExcludeEntity(val entity: EntityReference) : CollectionFilter

    /**
     * Exclude all entities present in another stored collection — i.e., set difference.
     * Used when one collection is a superset of another and you need the remainder
     * (e.g., GatherUntilMatch stores the match in both `storeMatch` and `storeRevealed`;
     * use this to compute "the rest of the revealed cards").
     *
     * @property otherCollectionName Name of the collection whose entities should be excluded
     */
    @SerialName("ExcludeOtherCollection")
    @Serializable
    data class ExcludeOtherCollection(val otherCollectionName: String) : CollectionFilter

    /**
     * Keep only entities that are currently in [zone].
     *
     * Pipeline collections track entity references, not the cards' live location, so a card
     * gathered into a collection can subsequently leave the zone it was in (e.g. an exiled
     * card cast for free during the same resolution moves to the stack). This filter re-reads
     * each card's current zone so a downstream step acts only on the cards still there.
     *
     * Used by "exile cards … you may cast it … if you don't, put that card into your hand"
     * effects (the Tarkir: Dragonstorm "…storm" enchantments): after the optional free-cast,
     * keep only the nonland card still in exile and move it to hand.
     *
     * @property zone The zone a card must currently be in to be kept.
     */
    @SerialName("InZone")
    @Serializable
    data class InZone(val zone: com.wingedsheep.sdk.core.Zone) : CollectionFilter
}

/**
 * Filter a named collection, splitting it into matching and non-matching subsets.
 *
 * This is a purely automatic filter (no player choice). The matching entities
 * are stored in [storeMatching]; non-matching entities are stored in [storeNonMatching]
 * if provided.
 *
 * @property from Name of the collection to filter
 * @property filter How to filter the collection
 * @property storeMatching Name of the collection to store entities that pass the filter
 * @property storeNonMatching Optional name to store entities that fail the filter
 */
@SerialName("FilterCollection")
@Serializable
data class FilterCollectionEffect(
    val from: String,
    val filter: CollectionFilter,
    val storeMatching: String,
    val storeNonMatching: String? = null
) : Effect {
    override val description: String = "Filter $from collection"
}

/**
 * Evaluate a [DynamicAmount] once and store it in pipeline `storedNumbers` under [name].
 * Subsequent effects in the same composite can read it via
 * [DynamicAmount.VariableReference] so the value does not drift when earlier sub-effects
 * mutate projected state.
 *
 * Used when an X-value must be computed from the current state once and then reused
 * across multiple sub-effects (draw, pump, etc.) that would otherwise see a re-projected
 * state with their own prior modifications already applied.
 */
@SerialName("StoreNumber")
@Serializable
data class StoreNumberEffect(
    val name: String,
    val amount: DynamicAmount
) : Effect {
    override val description: String = "Store ${amount.description} as $name"
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Read the name of the first card in a stored collection and store it under [storeAs] in
 * pipeline `chosenValues`. Subsequent effects can match cards against it via
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameEqualsChosen] (e.g.
 * `GameObjectFilter.namedFromVariable(storeAs)`).
 *
 * This is the "capture a card's name" counterpart to [ChooseOptionEffect] with
 * [OptionType.CARD_NAME]: instead of the player naming a card, the name is taken from a
 * card they selected. Used by Lobotomy ("you choose a card … search for all cards with
 * the same name"). A no-op when [from] is empty or missing.
 *
 * @property from Stored collection whose first card's name to capture.
 * @property storeAs Key under which the captured name is stored in `chosenValues`.
 */
@SerialName("StoreCardName")
@Serializable
data class StoreCardNameEffect(
    val from: String,
    val storeAs: String = "chosenCardName"
) : Effect {
    override val description: String = "Note the name of the $from card"
}
