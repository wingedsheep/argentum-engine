package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * You control enchanted permanent.
 * Used for Auras like Annex that steal control of the enchanted permanent.
 */
@SerialName("ControlEnchantedPermanent")
@Serializable
data object ControlEnchantedPermanent : StaticAbility {
    override val description: String = "You control enchanted permanent"
}

/**
 * A static ability that only applies when a condition is met.
 * Used for cards like Karakyk Guardian: "hexproof if it hasn't dealt damage yet"
 *
 * The engine checks the condition during state projection and only applies
 * the underlying ability's effect when the condition is true.
 *
 * @property ability The underlying static ability to apply when condition is met
 * @property condition The condition that must be true for the ability to apply
 */
@SerialName("ConditionalStaticAbility")
@Serializable
data class ConditionalStaticAbility(
    val ability: StaticAbility,
    val condition: Condition
) : StaticAbility {
    override val description: String = "${ability.description} ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newAbility = ability.applyTextReplacement(replacer)
        val newCondition = condition.applyTextReplacement(replacer)
        return if (newAbility !== ability || newCondition !== condition) copy(ability = newAbility, condition = newCondition) else this
    }
}

/**
 * Whenever enchanted land is tapped for mana, its controller adds additional mana.
 * Used for auras like Elvish Guidance: "Whenever enchanted land is tapped for mana,
 * its controller adds an additional {G} for each Elf on the battlefield."
 *
 * This is a triggered mana ability that resolves immediately (doesn't use the stack).
 * The engine checks for this ability after a mana ability on the enchanted land resolves.
 *
 * @property color The color of additional mana to produce. When `null`, the color is
 *   read from the aura's `ChosenColorComponent` at resolution (e.g., Shimmerwilds Growth).
 *   If the source has no chosen color, no mana is added (per Oracle ruling). Ignored when
 *   [anyColor] is true.
 * @property amount How much additional mana to produce (evaluated dynamically)
 * @property anyColor When true, the bonus is mana of any color the controller chooses
 *   (a real choice made each time the land is tapped) rather than a fixed/aura-chosen color.
 *   Used by Fertile Ground ("adds an additional one mana of any color"). On a manual tap the
 *   controller is prompted for the color; when auto-tapping for a cost the solver treats the
 *   bonus as flexible and supplies whatever color the cost needs.
 */
@SerialName("AdditionalManaOnTap")
@Serializable
data class AdditionalManaOnTap(
    val color: Color? = null,
    val amount: DynamicAmount,
    val anyColor: Boolean = false
) : StaticAbility {
    override val description: String =
        if (anyColor) "Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color"
        else "Whenever enchanted land is tapped for mana, its controller adds additional mana"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Replaces the mana produced by the enchanted land's own mana abilities with mana
 * of a specific color. The land still taps normally, but whatever color(s) it would
 * produce are swapped for [color] instead.
 *
 * When [color] is `null`, the replacement color is read from the aura's own
 * `ChosenColorComponent` (set via `EntersWithChoice(ChoiceType.COLOR)`) at resolution
 * time. If no color is chosen, no replacement happens and the land produces as normal.
 *
 * This is how Shimmerwilds Growth realises "Enchanted land is the chosen color":
 * a Mountain enchanted with Shimmerwilds Growth (Blue chosen) produces `{U}` when
 * tapped, not `{R}`. Combines naturally with [AdditionalManaOnTap] to stack bonuses
 * in the same color (e.g., `{U}` base + `{U}` bonus = `{U}{U}`).
 *
 * Applies to the source's *own* intrinsic mana abilities only — not to mana produced
 * by any other source.
 */
@SerialName("OverrideEnchantedLandManaColor")
@Serializable
data class OverrideEnchantedLandManaColor(
    val color: Color? = null
) : StaticAbility {
    override val description: String =
        if (color == null) "Enchanted land produces mana of the chosen color instead of its normal output"
        else "Enchanted land produces ${color.name.lowercase()} mana instead of its normal output"
}

/**
 * Whenever a permanent matching [sourceFilter] is tapped for mana, the tapping
 * player adds [amount] additional mana to their pool.
 *
 * Unifies the "Mana Flare on a filter" and "Badgermole Cub on creature taps" shapes:
 *
 * - **Lavaleaper** ("Whenever a player taps a basic land for mana, that player adds
 *   one mana of any type that land produced") →
 *   `AdditionalManaOnSourceTap(sourceFilter = GameObjectFilter.BasicLand, color = null)`.
 *   `color = null` means **mirror the produced color** — the bonus matches whatever
 *   color the source produced.
 *
 * - **Badgermole Cub** ("Whenever you tap a creature for mana, add an additional {G}") →
 *   `AdditionalManaOnSourceTap(sourceFilter = GameObjectFilter.Creature.youControl(),
 *   color = Color.GREEN)`. The "you tap" wording is captured by the filter's controller
 *   predicate: the source must be controlled by the static-ability controller, and since
 *   only that controller can activate the source's mana ability (mana-ability rules), the
 *   trigger only fires when "you" tap a matching creature.
 *
 * Triggered mana ability — resolves immediately without using the stack (Rule 605.1).
 * Filter matching uses projected state, so animated creature-lands count as creatures
 * and typeshifted lands count under their projected types. The filter's controller
 * predicate is evaluated against the static-ability source's projected controller (i.e.
 * `youControl` means "controlled by you, the controller of this static").
 *
 * @property sourceFilter Which permanents, when tapped for mana, trigger this bonus.
 *   Use `.youControl()` for the "Whenever you tap..." wording.
 * @property color The bonus mana color. `null` means mirror the color the source produced
 *   (used by Lavaleaper). When set, the bonus is always that color regardless of the source.
 * @property amount How many additional mana per tap (default 1).
 * @property rider An optional non-mana side effect resolved inline right after the bonus mana
 *   is added, controlled by the tapping player (so `EffectTarget.Controller` is the tapper and
 *   `EffectTarget.Self` is this static's source). Used by Overabundance ("…and this enchantment
 *   deals 1 damage to the player"). Like the bonus mana itself this is a triggered mana ability
 *   that resolves immediately without the stack, so the rider must not require targeting input.
 *   The rider runs only on the manual mana-ability path; auto-tapping for a cost adds the mirror
 *   mana via the solver but skips the rider, matching how the engine already treats mana-ability
 *   side effects (e.g. City of Brass's damage) during automatic payment.
 */
@SerialName("AdditionalManaOnSourceTap")
@Serializable
data class AdditionalManaOnSourceTap(
    val sourceFilter: GameObjectFilter,
    val color: Color? = null,
    val amount: DynamicAmount = DynamicAmount.Fixed(1),
    val rider: Effect? = null
) : StaticAbility {
    override val description: String = buildString {
        append("Whenever a ")
        append(sourceFilter.description)
        append(" is tapped for mana, ")
        if (color == null) {
            append("that player adds one mana of any type that source produced.")
        } else {
            append("add an additional {${color.symbol}}.")
        }
        if (rider != null) {
            append(" ")
            append(rider.description)
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = sourceFilter.applyTextReplacement(replacer)
        val newAmount = amount.applyTextReplacement(replacer)
        val newRider = rider?.applyTextReplacement(replacer)
        return if (newFilter !== sourceFilter || newAmount !== amount || newRider !== rider) {
            copy(sourceFilter = newFilter, amount = newAmount, rider = newRider)
        } else this
    }
}

/**
 * Lands matching [filter] produce one mana of a color of their controller's choice instead of
 * their normal mana ("instead of any other type"). The land still taps as a mana ability; only
 * the produced mana's color is replaced, and the controller chooses the color each time.
 *
 * Used by Pulse of Llanowar ("If a basic land you control is tapped for mana, it produces mana of
 * a color of your choice instead of any other type") with `filter = GameObjectFilter.BasicLand.youControl()`.
 *
 * This is a global mana-production replacement: it is checked whenever a land's mana ability
 * resolves (manual taps) and is honored by the [com.wingedsheep] ManaSolver when auto-tapping for
 * costs (a matched land is treated as a five-color source). Unlike [OverrideEnchantedLandManaColor]
 * (a per-aura fixed-color override) this is filter-based and the color is a free choice.
 *
 * @property filter Which lands have their produced mana color replaced (matched via projected
 *   state from the land controller's perspective, so `youControl()` means "you, the controller of
 *   this static, control the land").
 */
@SerialName("ReplaceLandManaColor")
@Serializable
data class ReplaceLandManaColor(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "If a ${filter.description} is tapped for mana, it produces mana of a color of its controller's choice instead of any other type"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Play with the top card of your library revealed.
 * You may play lands and cast spells from the top of your library.
 * Used for Future Sight.
 */
@SerialName("PlayFromTopOfLibrary")
@Serializable
data object PlayFromTopOfLibrary : StaticAbility {
    override val description: String =
        "Play with the top card of your library revealed. You may play lands and cast spells from the top of your library."
}

/**
 * You may cast spells matching a filter from the top of your library.
 * Unlike PlayFromTopOfLibrary, this only allows specific spell types (not all spells/lands).
 * Used for Precognition Field (instant and sorcery only).
 *
 * @property filter The filter that spells on top of library must match to be castable
 */
@SerialName("CastSpellTypesFromTopOfLibrary")
@Serializable
data class CastSpellTypesFromTopOfLibrary(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "You may cast ${filter.description} spells from the top of your library."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * You may look at the top card of your library any time.
 * Unlike PlayFromTopOfLibrary, this only reveals the top card privately to the controller,
 * not to all players. Used for Lens of Clarity, Vizier of the Menagerie, etc.
 */
@SerialName("LookAtTopOfLibrary")
@Serializable
data object LookAtTopOfLibrary : StaticAbility {
    override val description: String =
        "You may look at the top card of your library any time."
}

/**
 * Play with the top card of your library revealed (to all players), without granting any
 * permission to play it from there. Used for Goblin Spy.
 *
 * This is the public-reveal half of [PlayFromTopOfLibrary] with the play permission removed:
 * the controller's top card is shown to everyone, but it can only be played once it's drawn.
 * The engine treats it identically to [PlayFromTopOfLibrary] for *visibility* (the
 * ClientStateTransformer reveals the top card to all players), but unlike that ability it does
 * not appear in any cast/play-from-top permission path.
 */
@SerialName("RevealTopOfLibrary")
@Serializable
data object RevealTopOfLibrary : StaticAbility {
    override val description: String =
        "Play with the top card of your library revealed."
}

/**
 * You may play lands and cast spells matching a filter from the top of your library.
 * Unlike PlayFromTopOfLibrary, this restricts which spells can be cast (but always allows lands).
 * Used for Glarb, Calamity's Augur (mana value 4 or greater).
 *
 * @property spellFilter The filter that spells on top of library must match to be castable
 */
@SerialName("PlayLandsAndCastFilteredFromTopOfLibrary")
@Serializable
data class PlayLandsAndCastFilteredFromTopOfLibrary(
    val spellFilter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "You may play lands and cast spells matching ${spellFilter.description} from the top of your library."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * You may look at face-down creatures you don't control any time.
 * Reveals the identity of opponent's face-down creatures to the controller.
 * Used for Lens of Clarity.
 */
@SerialName("LookAtFaceDownCreatures")
@Serializable
data object LookAtFaceDownCreatures : StaticAbility {
    override val description: String =
        "You may look at face-down creatures you don't control any time."
}

/**
 * Your opponents play with their hands revealed.
 * Used for Seer's Vision and similar "opponents reveal hands" enchantments.
 *
 * This is a visibility-only static ability, the opponent-facing sibling of
 * [RevealTopOfLibrary]. Each opponent of this ability's controller plays with their
 * hand publicly visible to that controller. It grants no other game effect.
 *
 * The engine treats it purely as a masking concern: the [com.wingedsheep] ClientStateTransformer
 * reveals an opponent's hand to a viewing player who controls a permanent with this
 * ability. No projector/state change is involved.
 */
@SerialName("OpponentsPlayWithHandsRevealed")
@Serializable
data object OpponentsPlayWithHandsRevealed : StaticAbility {
    override val description: String = "Your opponents play with their hands revealed."
}

/**
 * You may cast this card from specified zones (e.g., graveyard, exile).
 * This is an intrinsic property of the card, checked when the card is in a matching zone.
 * Used for Squee, the Immortal (graveyard + exile).
 *
 * @property zones The zones from which this card may be cast
 */
@SerialName("MayCastSelfFromZones")
@Serializable
data class MayCastSelfFromZones(
    val zones: List<Zone>
) : StaticAbility {
    override val description: String = "You may cast this card from ${
        zones.joinToString(" or ") { it.displayName }
    }."
}

/**
 * During each of your turns, you may play a land and cast a permanent spell of each
 * permanent type from your graveyard. Each permanent type (artifact, creature, enchantment,
 * land, planeswalker) can only be used once per turn per source of this ability.
 * If a card has multiple permanent types, you choose one as you play it.
 * Used for Muldrotha, the Gravetide.
 */
@SerialName("MayPlayPermanentsFromGraveyard")
@Serializable
data object MayPlayPermanentsFromGraveyard : StaticAbility {
    override val description: String =
        "During each of your turns, you may play a land and cast a permanent spell of each permanent type from your graveyard."
}

/**
 * You may play lands from your graveyard.
 * Used for Crucible of Worlds / Icetill Explorer style effects.
 *
 * Unlike [MayPlayPermanentsFromGraveyard] (Muldrotha), no per-turn usage tracking
 * is needed — the land-drop counter already limits how many lands can be played.
 * Multiple copies are redundant.
 */
@SerialName("MayPlayLandsFromGraveyard")
@Serializable
data object MayPlayLandsFromGraveyard : StaticAbility {
    override val description: String = "You may play lands from your graveyard"
}

/**
 * Prevents all players from cycling cards.
 * Used for Stabilizer: "Players can't cycle cards."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * when determining if cycling/typecycling is a legal action.
 */
@SerialName("PreventCycling")
@Serializable
data object PreventCycling : StaticAbility {
    override val description: String = "Players can't cycle cards"
}

/**
 * Activated abilities of permanents matching [filter] can't be activated.
 *
 * Used for Cursed Totem ("Activated abilities of creatures can't be activated") and
 * similar global denial effects (Damping Matrix, Pithing Needle's spiritual ancestor).
 * Both mana and non-mana activated abilities are blocked, including abilities granted
 * by static effects (e.g., basic-land mana abilities granted to creature-lands while
 * they are creatures).
 *
 * Loyalty abilities of planeswalkers and ability costs that only animate a noncreature
 * permanent (e.g., a Vehicle's Crew ability while it is still a Vehicle, not yet a
 * creature) are unaffected — once the source becomes a creature its activated abilities
 * are blocked too. This is enforced by filter-matching against projected state, so
 * type-changing effects flow through correctly.
 */
@SerialName("PreventActivatedAbilities")
@Serializable
data class PreventActivatedAbilities(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "Activated abilities of ${filter.description} can't be activated"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Prevents mana pools from emptying as steps and phases end.
 * Used for Upwelling: "Players don't lose unspent mana as steps and phases end."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * during mana pool cleanup. While any permanent with this ability is on the battlefield,
 * mana pools are not emptied.
 */
@SerialName("PreventManaPoolEmptying")
@Serializable
data object PreventManaPoolEmptying : StaticAbility {
    override val description: String = "Players don't lose unspent mana as steps and phases end"
}

/**
 * Removes the maximum hand size limit for the controller.
 * Used for cards like Thought Vessel and Reliquary Tower: "You have no maximum hand size."
 *
 * The engine checks for this static ability during the cleanup step. If the active player
 * controls any permanent on the battlefield with this ability, the discard-down-to-hand-size
 * action is skipped.
 */
@SerialName("NoMaximumHandSize")
@Serializable
data object NoMaximumHandSize : StaticAbility {
    override val description: String = "You have no maximum hand size"
}

/**
 * Reveal the first card the controller draws each turn.
 * Used for Primitive Etchings and similar "reveal as you draw" effects.
 *
 * The engine checks for this static ability during draws. When the controller
 * draws their first card of a turn and this ability is active, the drawn card
 * is revealed (a CardRevealedFromDrawEvent is emitted). Other triggered abilities
 * can then trigger off that reveal event.
 */
@SerialName("RevealFirstDrawEachTurn")
@Serializable
data object RevealFirstDrawEachTurn : StaticAbility {
    override val description: String = "Reveal the first card you draw each turn"
}

/**
 * Replaces land mana production when a land would produce two or more mana.
 * Used for Damping Sphere: "If a land is tapped for two or more mana, it produces {C} instead
 * of any other type and amount."
 *
 * This is a global replacement effect — it applies to all lands for all players.
 * The engine checks for this when resolving mana abilities from lands. If the mana ability
 * would add 2+ total mana, it instead adds only one colorless mana.
 * The ManaSolver also accounts for this when calculating available mana sources.
 */
@SerialName("DampLandManaProduction")
@Serializable
data object DampLandManaProduction : StaticAbility {
    override val description: String = "If a land is tapped for two or more mana, it produces {C} instead of any other type and amount"
}

/**
 * Untap all permanents you control during each other player's untap step.
 * Used for Seedborn Muse and similar effects.
 *
 * The engine checks for this static ability during the untap step. When the active
 * player is not the controller of a permanent with this ability, all permanents
 * controlled by the ability's controller are untapped as well.
 */
@SerialName("UntapDuringOtherUntapSteps")
@Serializable
data object UntapDuringOtherUntapSteps : StaticAbility {
    override val description: String = "Untap all permanents you control during each other player's untap step"
}

/**
 * Untap permanents matching a filter you control during each other player's untap step.
 * Used for Ivorytusk Fortress (creatures with +1/+1 counters) and similar effects.
 *
 * The engine checks for this static ability during the untap step. When the active
 * player is not the controller of a permanent with this ability, permanents matching
 * the filter controlled by the ability's controller are untapped.
 */
@SerialName("UntapFilteredDuringOtherUntapSteps")
@Serializable
data class UntapFilteredDuringOtherUntapSteps(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String = "Untap each ${filter.description} you control during each other player's untap step"
}

/**
 * You may activate loyalty abilities of planeswalkers you control an extra time each turn.
 * Used for Oath of Teferi: "You may activate the loyalty abilities of planeswalkers you control
 * twice each turn rather than only once."
 *
 * The engine checks for this static ability on the controller's battlefield when validating
 * planeswalker loyalty ability activations. Multiple copies do NOT stack — the maximum is
 * always two activations per planeswalker per turn regardless of how many copies are controlled.
 */
@SerialName("ExtraLoyaltyActivation")
@Serializable
data object ExtraLoyaltyActivation : StaticAbility {
    override val description: String = "You may activate loyalty abilities of planeswalkers you control twice each turn rather than only once"
}

/**
 * Whether [AdditionalETBOrLTBTriggers] watches the entering side, the leaving side, or both
 * of a permanent's battlefield transit.
 */
@Serializable
enum class BattlefieldDirection {
    /** Permanent entered the battlefield (`ZoneChangeEvent.toZone == Zone.BATTLEFIELD`). */
    @SerialName("Entering")
    ENTERING,

    /** Permanent left the battlefield (`ZoneChangeEvent.fromZone == Zone.BATTLEFIELD`). */
    @SerialName("Leaving")
    LEAVING
}

/**
 * When a permanent matching [filter] crosses the battlefield boundary in one of [directions],
 * triggered abilities of permanents controlled by this ability's controller that fired from
 * that event trigger an additional time per copy. CR 603.2d ("An ability may state that a
 * triggered ability triggers additional times").
 *
 * Models the Panharmonicon family (entering only) and, with `LEAVING` added, Gandalf-the-White-
 * style "entering or leaving the battlefield" doublers. Concrete shapes:
 *  - Panharmonicon — `filter = GameObjectFilter.Any` (creature/artifact only in oracle text;
 *    use a composed filter), default `mustBeYouControl = true`, default `directions = {ENTERING}`.
 *  - Naban, Dean of Iteration — `filter = Creature.withSubtype("Wizard")`, default flags.
 *  - Traveling Chocobo — `filter = Land or Creature.withSubtype("Bird")`, default flags.
 *  - Starfield Vocalist — `filter = GameObjectFilter.Any`, `mustBeYouControl = false`.
 *  - Gandalf the White — `filter = Artifact or Any.legendary()`, `mustBeYouControl = false`,
 *    `directions = setOf(ENTERING, LEAVING)`.
 *
 * @property filter Which permanent's entering/leaving counts as the "cause" event.
 * @property mustBeYouControl When true (default), the cause permanent must be controlled by
 *   this ability's controller — matches the typical "X you control entering" wording. Set to
 *   false when the oracle text omits the "under your control" restriction on the cause
 *   permanent (Starfield Vocalist, Gandalf the White). The triggered ability's source permanent
 *   is always required to be controlled by this ability's controller regardless of this flag —
 *   that part is the "of a permanent you control" half of the oracle text and isn't optional.
 * @property directions Which transit directions are watched. Defaults to `{ENTERING}` (the
 *   Panharmonicon shape). Add `LEAVING` to cover "entering or leaving the battlefield" wording
 *   (Gandalf the White) or stand-alone "leaves the battlefield" doublers.
 *
 * Multiple copies are additive: N copies add N extra firings of each affected trigger.
 */
@SerialName("AdditionalETBOrLTBTriggers")
@Serializable
data class AdditionalETBOrLTBTriggers(
    val filter: GameObjectFilter,
    val mustBeYouControl: Boolean = true,
    val directions: Set<BattlefieldDirection> = setOf(BattlefieldDirection.ENTERING),
    override val description: String = "Triggered abilities trigger an additional time"
) : StaticAbility {
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * If a triggered ability of a permanent matching [sourceFilter] you control triggers,
 * it triggers an additional time.
 *
 * Models Twinflame Travelers ("If a triggered ability of another Elemental you control
 * triggers, it triggers an additional time") and similar effects that double *all* triggers
 * (not just ETB) for a filtered group of permanents.
 *
 * The duplicated trigger inherits the original's source, controller, and trigger context,
 * so it resolves identically to the original — players choose new targets independently
 * for each copy.
 *
 * Multiple copies are additive: N copies cause N+1 total firings.
 *
 * @property sourceFilter Which permanents' triggers get doubled (matched via projected state).
 * @property excludeSelf When true, the static ability's own source is excluded from the filter
 *   (matches the "another" wording in oracle text). Defaults to true.
 */
@SerialName("AdditionalSourceTriggers")
@Serializable
data class AdditionalSourceTriggers(
    val sourceFilter: GameObjectFilter,
    val excludeSelf: Boolean = true,
    override val description: String = "If a triggered ability of another permanent matching the filter you control triggers, it triggers an additional time"
) : StaticAbility {
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = sourceFilter.applyTextReplacement(replacer)
        return if (newFilter !== sourceFilter) copy(sourceFilter = newFilter) else this
    }
}

/**
 * If a creature matching [attackerFilter] attacking causes a triggered ability of a permanent
 * you control to trigger, that ability triggers an additional time.
 *
 * This is the "attack-cause" analogue of [AdditionalETBOrLTBTriggers]: instead of doubling triggers
 * caused by a permanent entering the battlefield, it doubles triggers caused by a creature being
 * declared as an attacker (the attackers-declared event). Models Windcrag Siege's Mardu mode:
 * "If a creature attacking causes a triggered ability of a permanent you control to trigger, that
 * ability triggers an additional time."
 *
 * Only triggers belonging to permanents controlled by this ability's controller are doubled, and
 * only triggers that fired from the same attackers-declared event (their triggering entity is one
 * of the declared attackers matching [attackerFilter]). The duplicated trigger inherits the
 * original's source, controller, and triggering attacker, so players choose new targets
 * independently for each copy.
 *
 * [attackerFilter] restricts which attacking creatures cause the doubling. Defaults to
 * [GameObjectFilter.Any] — any creature attacking, matching Windcrag Siege's unrestricted wording.
 *
 * Multiple copies are additive: N copies add N extra firings of each affected trigger.
 */
@SerialName("AdditionalAttackTriggers")
@Serializable
data class AdditionalAttackTriggers(
    val attackerFilter: GameObjectFilter = GameObjectFilter.Any,
    override val description: String = "If a creature attacking causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time"
) : StaticAbility {
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = attackerFilter.applyTextReplacement(replacer)
        return if (newFilter !== attackerFilter) copy(attackerFilter = newFilter) else this
    }
}

/**
 * You may play additional lands on each of your turns.
 * Used for permanents like Hugs, Grisly Guardian and Oracle of Mul Daya.
 *
 * This is a continuous effect — the bonus applies as long as the permanent is on the
 * battlefield. If the permanent enters mid-turn, the extra land drop is immediately
 * available. If it leaves, the bonus is immediately lost.
 *
 * Multiple copies are additive: two copies yield two additional land drops.
 *
 * @property count The number of additional land drops granted each turn (default 1)
 */
@SerialName("GrantAdditionalLandDrop")
@Serializable
data class GrantAdditionalLandDrop(
    val count: Int = 1
) : StaticAbility {
    override val description: String =
        "You may play ${if (count == 1) "an additional land" else "$count additional lands"} on each of your turns"
}

/**
 * If a source you control would deal noncombat damage to an opponent or a permanent
 * an opponent controls, it deals that much damage plus the bonus amount instead.
 *
 * Used for Artist's Talent Level 3 and similar "noncombat damage amplification" effects.
 * This is a static ability on a permanent — the bonus applies as long as the permanent
 * is on the battlefield with this ability active.
 *
 * @property bonusAmount The flat bonus to add to noncombat damage dealt to opponents
 */
@SerialName("NoncombatDamageBonus")
@Serializable
data class NoncombatDamageBonus(
    val bonusAmount: Int
) : StaticAbility {
    override val description: String =
        "If a source you control would deal noncombat damage to an opponent or a permanent an opponent controls, it deals that much damage plus $bonusAmount instead"
}
