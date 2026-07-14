package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an additional cost that must be paid when casting a spell.
 * This is separate from mana costs and includes things like:
 * - Sacrifice a creature (Natural Order)
 * - Discard a card (Force of Will)
 * - Pay life (Phyrexian mana)
 *
 * Additional costs are declared in the CardScript and validated/paid during casting.
 *
 * The payable things shared with the other cost contexts (sacrifice, discard, pay life, exile from a
 * zone, tap permanents) live in the [CostAtom] vocabulary and are carried here by [Atom]. The remaining
 * subtypes are genuinely casting-context-specific: alternative "X or pay mana" shapes (Blight/Behold),
 * pipeline-storage flow (Behold/ExileFromStorage/Composite), per-target life, variable exile, and the
 * cross-zone [ChooseEntity] pick.
 */
@Serializable
sealed interface AdditionalCost : TextReplaceable<AdditionalCost> {
    /** Human-readable description of the cost */
    val description: String

    companion object {
        /** "behold a [filter] and exile it" — Behold + ExileFromStorage composed as one cost. */
        fun BeholdAndExile(
            filter: GameObjectFilter,
            count: Int = 1,
            storeAs: String = "beheld"
        ): Composite = Composite(listOf(
            Behold(filter = filter, count = count, storeAs = storeAs),
            ExileFromStorage(from = storeAs, linkToSource = true)
        ))
    }

    /**
     * A single shared payable thing — see [CostAtom]. Covers the additional costs that mean the same
     * here as in any other context: sacrifice a permanent (Natural Order), discard cards (Force of
     * Will), pay life (Phyrexian mana), exile cards from a zone, tap permanents. Additional-cost text
     * leads a clause, so the description is the atom's phrase with its first letter capitalized.
     */
    @SerialName("AdditionalAtom")
    @Serializable
    data class Atom(val atom: CostAtom) : AdditionalCost {
        override val description: String get() = atom.description.replaceFirstChar { it.uppercase() }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newAtom = atom.applyTextReplacement(replacer)
            return if (newAtom !== atom) copy(atom = newAtom) else this
        }
    }

    /**
     * Pay [amountPerTarget] life for each target chosen by this spell.
     * Example: "This spell costs 3 life more to cast for each target." (Phyrexian Purge)
     *
     * Always payable at enumeration time — if the player has too little life, they can
     * still cast the spell by choosing zero targets (when the spell's target requirement
     * is `optional = true`). The actual amount is computed from `action.targets.size` at
     * cast resolution.
     */
    @SerialName("PayLifePerTarget")
    @Serializable
    data class PayLifePerTarget(
        val amountPerTarget: Int
    ) : AdditionalCost {
        override val description: String = "This spell costs $amountPerTarget life more to cast for each target"
    }

    /**
     * Exile a variable number of cards from a zone as an additional cost.
     * The player chooses how many matching cards to exile (at least [minCount]).
     * Example: "Exile X creature cards from your graveyard" for Chill Haunting
     *
     * @property minCount Minimum number of cards to exile (default 1)
     * @property filter Which cards can be exiled
     * @property fromZone Zone to exile from
     */
    @SerialName("ExileVariableCards")
    @Serializable
    data class ExileVariableCards(
        val minCount: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val fromZone: CostZone = CostZone.GRAVEYARD
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Exile X ")
            append(filter.description)
            append("s from your ${fromZone.description}")
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Sacrifice any number of permanents matching the given filter as an additional cost.
     * Each sacrifice reduces the spell's generic mana cost by [costReductionPerCreature].
     * Example: "You may sacrifice any number of creatures. This spell costs {2} less for each creature sacrificed."
     *
     * @property filter Which permanents can be sacrificed
     * @property costReductionPerCreature Generic mana reduction per sacrificed creature
     */
    @SerialName("SacrificeCreaturesForCostReduction")
    @Serializable
    data class SacrificeCreaturesForCostReduction(
        val filter: GameObjectFilter = GameObjectFilter.Creature,
        val costReductionPerCreature: Int = 2
    ) : AdditionalCost {
        override val description: String = "You may sacrifice any number of creatures. This spell costs {$costReductionPerCreature} less to cast for each creature sacrificed this way."
        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Forage: exile three cards from your graveyard or sacrifice a Food.
     * Used by Bloomburrow cards as an additional cost.
     */
    @SerialName("Forage")
    @Serializable
    data object Forage : AdditionalCost {
        override val description: String = "Forage (Exile three cards from your graveyard or sacrifice a Food)"
    }

    /**
     * Blight X (variable): the caster declares X at cast time, puts X -1/-1
     * counters on a creature they control, and X is exposed to the spell's
     * effects via `DynamicAmount.CastChoice(ChoiceSlot.BLIGHT_AMOUNT)`.
     *
     * X is bounded by the greatest toughness among creatures the caster controls
     * (per CR text on Soul Immolation). The cap is computed at cast-enumeration
     * time; if the caster controls no creatures, the only legal X is [minCount]
     * (default 0 — i.e., the cost is silently zero, no creature choice required).
     *
     * @property minCount Minimum X (default 0 — caster may always declare X = 0)
     */
    @SerialName("BlightVariable")
    @Serializable
    data class BlightVariable(
        val minCount: Int = 0
    ) : AdditionalCost {
        override val description: String =
            "blight X. X can't be greater than the greatest toughness among creatures you control"
    }

    /**
     * Pay X life (variable): the caster declares X at cast time and pays X life as an additional
     * cost. X is exposed to the spell's effects via the resolution context's X value (the same slot
     * read by `DynamicAmount.XValue` and `CardPredicate.ManaValueAtMostX` / `ManaValueEqualsX`), so a
     * card like Vicious Rivalry ("pay X life; destroy all artifacts and creatures with mana value X
     * or less") sources its single X from this one cost.
     *
     * X is bounded by the caster's current life total (you can never pay more life than you have).
     * The cap is computed at cast-enumeration time; the only forced minimum is [minCount] (default 0
     * — the caster may always declare X = 0).
     *
     * A card carrying this cost must NOT also have an `{X}` in its mana cost — both write the same X
     * slot on the spell, so they would collide. The two are mutually exclusive per card.
     *
     * @property minCount Minimum X (default 0 — caster may always declare X = 0)
     */
    @SerialName("PayXLife")
    @Serializable
    data class PayXLife(
        val minCount: Int = 0
    ) : AdditionalCost {
        override val description: String = "Pay X life"
    }

    /**
     * Pay life equal to the mana value of the spell being cast. Auto-paid (no player choice).
     * Used as the substitute cost in "pay life equal to its mana value rather than pay its
     * mana cost" permissions — Valgavoth, Terror Eater (paired with a play-from-exile grant
     * whose mana cost is waived), and Bolas's Citadel-style effects. The amount is computed at
     * cast time from the cast card's [com.wingedsheep.sdk.model.CardDefinition] mana value.
     */
    @SerialName("PayLifeEqualToManaValueOfSpell")
    @Serializable
    data object PayLifeEqualToManaValueOfSpell : AdditionalCost {
        override val description: String = "pay life equal to its mana value"
    }

    /**
     * Blight N or pay additional mana: the caster must either put N -1/-1 counters on a creature
     * they control, or pay extra mana on top of the spell's base mana cost.
     * Used by Lorwyn Eclipsed cards (e.g., Wild Unraveling).
     *
     * The enumerator produces two legal actions: one for the blight path (base cost + creature selection)
     * and one for the pay path (base cost + [alternativeManaCost]).
     *
     * @property blightAmount Number of -1/-1 counters to place
     * @property alternativeManaCost Extra mana to pay instead of blighting (e.g., "{1}")
     */
    @SerialName("BlightOrPay")
    @Serializable
    data class BlightOrPay(
        val blightAmount: Int,
        val alternativeManaCost: String
    ) : AdditionalCost {
        override val description: String =
            if (alternativeManaCost.isBlank()) "you may blight $blightAmount"
            else "Blight $blightAmount or pay $alternativeManaCost"
    }

    /**
     * Behold: choose a matching permanent you control or reveal a matching card from your hand.
     * Used by Lorwyn Eclipsed cards.
     *
     * Stores the chosen card IDs in [AdditionalCostPayment.beheldCards] and populates
     * pipeline storage under [storeAs] so downstream costs (e.g., [ExileFromStorage])
     * or effects can reference them.
     *
     * @property filter Which cards/permanents can be beheld (e.g., Elf, Kithkin, Goblin)
     * @property count Number of cards to behold
     * @property storeAs Pipeline storage key for the chosen cards
     */
    @SerialName("Behold")
    @Serializable
    data class Behold(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1,
        val storeAs: String = "beheld"
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Behold ")
            if (count == 1) {
                val filterDesc = filter.description
                val article = if (filterDesc.firstOrNull()?.lowercase() in listOf("a", "e", "i", "o", "u")) "an" else "a"
                append("$article ")
                append(filterDesc)
            } else {
                append("$count ")
                append(filter.description)
                append("s")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Behold a matching card or pay additional mana: the caster must either behold
     * (choose a matching permanent they control or reveal a matching card from their hand)
     * or pay extra mana on top of the spell's base mana cost. Unlike [BeholdAndExile],
     * this does not exile the beheld card.
     * Used by Lorwyn Eclipsed cards (e.g., Lys Alana Dignitary).
     *
     * The enumerator produces two legal actions: one for the behold path (base cost +
     * card selection) and one for the pay path (base cost + [alternativeManaCost]).
     *
     * @property filter Which cards/permanents can be beheld
     * @property alternativeManaCost Extra mana to pay instead of beholding (e.g., "{2}")
     * @property storeAs Pipeline storage key for the chosen cards (unused by cost itself,
     *   reserved for downstream composition)
     */
    @SerialName("BeholdOrPay")
    @Serializable
    data class BeholdOrPay(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val alternativeManaCost: String,
        val storeAs: String = "beheld"
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Behold ")
            val filterDesc = filter.description
            val article = if (filterDesc.firstOrNull()?.lowercase() in listOf("a", "e", "i", "o", "u")) "an" else "a"
            append("$article ")
            append(filterDesc)
            append(" or pay ")
            append(alternativeManaCost)
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Exile [exileCount] cards matching [filter] from your graveyard, or pay additional mana:
     * the caster must either exile that many matching cards from their graveyard, or pay extra mana
     * on top of the spell's base mana cost. The sibling of [BlightOrPay] / [BeholdOrPay] for the
     * "exile from graveyard or pay" shape (e.g. Soaring Stoneglider — "exile two cards from your
     * graveyard or pay {1}{W}").
     *
     * The enumerator produces up to two legal actions: one for the exile path (base cost + card
     * selection from the graveyard) and one for the pay path (base cost + [alternativeManaCost]).
     * The exile path is only offered when the graveyard holds at least [exileCount] matching cards.
     * The chosen path is recovered at payment time from whether
     * [AdditionalCostPayment.exiledCards] is non-empty.
     *
     * @property exileCount How many matching cards to exile from the graveyard
     * @property alternativeManaCost Extra mana to pay instead of exiling (e.g., "{1}{W}")
     * @property filter Which graveyard cards qualify (default any card)
     */
    @SerialName("ExileFromGraveyardOrPay")
    @Serializable
    data class ExileFromGraveyardOrPay(
        val exileCount: Int,
        val alternativeManaCost: String,
        val filter: GameObjectFilter = GameObjectFilter.Any,
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Exile $exileCount ")
            append(filter.description)
            append(if (exileCount == 1) " card from your graveyard or pay " else " cards from your graveyard or pay ")
            append(alternativeManaCost)
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Sacrifice [count] permanent(s) matching [filter] you control, or pay additional mana: the
     * caster must either sacrifice that many matching permanents, or pay extra mana on top of the
     * spell's base mana cost. The sibling of [BlightOrPay] / [BeholdOrPay] /
     * [ExileFromGraveyardOrPay] for the "sacrifice a permanent or pay" shape (e.g. Louisoix's
     * Sacrifice — "sacrifice a legendary creature or pay {2}").
     *
     * The enumerator produces up to two legal actions: one for the sacrifice path (base cost +
     * permanent selection from the battlefield, surfaced with `costType = "SacrificePermanent"`
     * like Natural Order's plain sacrifice cost) and one for the pay path (base cost +
     * [alternativeManaCost]). The sacrifice path is only offered when the caster controls at least
     * [count] permanents matching [filter]. The chosen path is recovered at payment time from
     * whether [AdditionalCostPayment.sacrificedPermanents] is non-empty.
     *
     * @property filter Which permanents you control qualify (e.g. legendary creature)
     * @property alternativeManaCost Extra mana to pay instead of sacrificing (e.g. "{2}")
     * @property count How many matching permanents to sacrifice on the sacrifice path
     */
    @SerialName("SacrificeOrPay")
    @Serializable
    data class SacrificeOrPay(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val alternativeManaCost: String,
        val count: Int = 1,
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Sacrifice ")
            if (count == 1) {
                val filterDesc = filter.description
                val article = if (filterDesc.firstOrNull()?.lowercase() in listOf("a", "e", "i", "o", "u")) "an" else "a"
                append("$article ")
                append(filterDesc)
            } else {
                append("$count ")
                append(filter.description)
                append("s")
            }
            append(" or pay ")
            append(alternativeManaCost)
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Exile cards from a named pipeline collection and optionally link them to the
     * source spell/permanent via LinkedExileComponent.
     *
     * General-purpose: consumes whatever a preceding cost stored under [from].
     * Example: Behold stores as "beheld", then ExileFromStorage("beheld") exiles those cards.
     *
     * @property from Pipeline storage key to read card IDs from
     * @property linkToSource Whether to add LinkedExileComponent (for LTB return patterns)
     */
    @SerialName("ExileFromStorage")
    @Serializable
    data class ExileFromStorage(
        val from: String,
        val linkToSource: Boolean = false
    ) : AdditionalCost {
        override val description: String = "Exile the chosen card"
    }

    /**
     * A composite additional cost that groups multiple atomic costs into a single logical cost.
     * The engine processes the steps in order, with pipeline storage flowing between them.
     *
     * Example: "behold an Elf and exile it" is [Behold] + [ExileFromStorage] composed together.
     */
    @SerialName("Composite")
    @Serializable
    data class Composite(
        val steps: List<AdditionalCost>
    ) : AdditionalCost {
        override val description: String = steps.joinToString(", ") { it.description }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newSteps = steps.map { it.applyTextReplacement(replacer) }
            return if (newSteps.zip(steps).any { (a, b) -> a !== b }) copy(steps = newSteps) else this
        }
    }

    /**
     * Choose one entity from any of the zones declared in [zoneFilters], applying
     * the matching filter for that zone, without moving it. The chosen entity ID
     * is recorded in [AdditionalCostPayment.beheldCards] and surfaced to the
     * resolution context under [storeAs] via the spell's pipeline storage.
     * Downstream effects can reference the chosen entity via
     * [com.wingedsheep.sdk.scripting.values.EntityReference.FromCostStorage].
     *
     * This is the silent sibling of [Behold]: same general shape (filter the
     * candidates, record one pick under a `storeAs` key) but **no reveal
     * semantics** for hand-zone choices, and the caller decides which zones to
     * search and which filter applies in each zone. Use it for cost shapes like
     * "choose a creature you control or a warped creature card you own in
     * exile" (Close Encounter, CR 702.185b), where Behold's hand-reveal
     * baggage doesn't apply and the per-zone filters differ.
     *
     * Per-zone iteration already restricts to the caster's slice of each zone
     * (`getBattlefieldControlledBy(playerId)` for the battlefield, `ZoneKey(playerId, zone)`
     * for hidden / card zones), so the filters don't need to redundantly assert
     * "you control" / "you own".
     *
     * @property zoneFilters Map from each zone to search to the filter that
     *   gates candidates from that zone. An empty map means there are no
     *   valid choices (the cost is unpayable).
     * @property storeAs Pipeline-storage key under which the chosen entity ID
     *   is exposed at resolution time.
     * @property captureSnapshot When true and the chosen entity is on the
     *   battlefield at cost-pay time, capture a [EntitySnapshot] so power /
     *   toughness / subtypes / controller can still be read after the entity
     *   leaves between cost-pay and resolution (Rule 112.7a; ruling on Close
     *   Encounter).
     */
    @SerialName("ChooseEntity")
    @Serializable
    data class ChooseEntity(
        val zoneFilters: Map<Zone, GameObjectFilter> =
            mapOf(Zone.BATTLEFIELD to GameObjectFilter.Any),
        val storeAs: String = "chosen",
        val captureSnapshot: Boolean = false,
        /**
         * Override the auto-generated player-facing description. Cross-zone
         * unions often have naturalized oracle text ("a creature you control
         * or a warped creature card you own in exile") that the
         * filter-description machinery can't reconstruct from
         * [zoneFilters] alone — pass the oracle wording verbatim here.
         */
        val descriptionOverride: String? = null,
    ) : AdditionalCost {
        override val description: String = descriptionOverride ?: buildString {
            append("choose ")
            val parts = zoneFilters.entries.map { (_, filter) ->
                val filterDesc = filter.description
                val article = if (filterDesc.firstOrNull()?.lowercase() in listOf("a", "e", "i", "o", "u")) "an" else "a"
                "$article $filterDesc"
            }
            append(parts.joinToString(" or "))
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            var changed = false
            val replaced = zoneFilters.mapValues { (_, filter) ->
                val new = filter.applyTextReplacement(replacer)
                if (new !== filter) changed = true
                new
            }
            return if (changed) copy(zoneFilters = replaced) else this
        }
    }
}

/**
 * Zones that cards can be exiled from as an additional cost.
 */
@Serializable
enum class CostZone(val description: String) {
    HAND("hand"),
    GRAVEYARD("graveyard"),
    LIBRARY("library"),
    BATTLEFIELD("battlefield");

    /** The engine [Zone] this cost-zone maps to. */
    fun toZone(): Zone = when (this) {
        HAND -> Zone.HAND
        GRAVEYARD -> Zone.GRAVEYARD
        LIBRARY -> Zone.LIBRARY
        BATTLEFIELD -> Zone.BATTLEFIELD
    }
}

/**
 * Represents the payment of additional costs.
 * This is included in the cast spell action to record what was paid.
 */
@Serializable
data class AdditionalCostPayment(
    /** Permanents that were sacrificed */
    val sacrificedPermanents: List<EntityId> = emptyList(),

    /** Cards that were discarded */
    val discardedCards: List<EntityId> = emptyList(),

    /** Life that was paid */
    val lifePaid: Int = 0,

    /** Cards that were exiled */
    val exiledCards: List<EntityId> = emptyList(),

    /** Cards chosen via Behold (from battlefield or hand) */
    val beheldCards: List<EntityId> = emptyList(),

    /** Permanents that were tapped */
    val tappedPermanents: List<EntityId> = emptyList(),

    /** Permanents that were returned to hand */
    val bouncedPermanents: List<EntityId> = emptyList(),

    /** Creature that received -1/-1 counters via Blight */
    val blightTargets: List<EntityId> = emptyList(),

    /**
     * X chosen for [AdditionalCost.BlightVariable] — the number of -1/-1
     * counters placed on [blightTargets] (singular target). Zero when no
     * variable-blight cost is in play, or when the player chose X = 0.
     */
    val blightAmount: Int = 0,

    /**
     * X chosen for [AdditionalCost.PayXLife] — the amount of life paid as the cost.
     * Zero when no pay-X-life cost is in play, or when the player chose X = 0.
     */
    val payXLifeAmount: Int = 0,

    /**
     * Distributed counter removals for costs where each entry removes [count]
     * counters of `counterType` from `entityId`. The engine validates that the sum
     * matches the cost's totalCount and that each creature has enough of each type.
     */
    val distributedCounterRemovals: List<DistributedCounterRemoval> = emptyList()
) {
    /** Check if any costs were paid */
    val isEmpty: Boolean
        get() = sacrificedPermanents.isEmpty() &&
                discardedCards.isEmpty() &&
                lifePaid == 0 &&
                exiledCards.isEmpty() &&
                beheldCards.isEmpty() &&
                tappedPermanents.isEmpty() &&
                bouncedPermanents.isEmpty() &&
                blightTargets.isEmpty() &&
                blightAmount == 0 &&
                payXLifeAmount == 0 &&
                distributedCounterRemovals.isEmpty()

    companion object {
        val NONE = AdditionalCostPayment()
    }
}

/**
 * A single removal entry for distributed counter-removal costs.
 * Remove [count] counters of [counterType] from [entityId].
 *
 * `counterType` is the canonical symbol (e.g. `"+1/+1"`, `"-1/-1"`, `"stun"`)
 * — the same string keys used in [CounterRemovalCreatureInfo.availableCountersByType]
 * — not the [CounterType] enum name. Stored as a string so the wire format
 * stays human-friendly and matches what the engine emits in those DTOs; the
 * engine resolves it back to a [CounterType] when paying the cost.
 */
@Serializable
data class DistributedCounterRemoval(
    val entityId: EntityId,
    val counterType: String,
    val count: Int
)
