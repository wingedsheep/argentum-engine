package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull

/*
 * Shape queries over an activated ability's [AbilityCost], shared by the activation stages
 * (validation, the choice pauses, payment, stack placement). Each looks at the top-level cost and one
 * level into an [AbilityCost.Composite], exactly as the activation handler always has.
 */

/** The first [CostAtom.TapPermanents] atom anywhere in this cost, or null if it has none. */
internal fun AbilityCost.firstTapPermanentsAtomOrNull(): CostAtom.TapPermanents? = when (this) {
    is AbilityCost.Atom -> atom as? CostAtom.TapPermanents
    is AbilityCost.Composite -> costs.firstNotNullOfOrNull { it.firstTapPermanentsAtomOrNull() }
    else -> null
}

/**
 * The first [CostAtom.ExileFromGraveyardForTotal] atom anywhere in this cost, or null.
 * A cost carries at most one — its selection is what `CardSource.ExiledAsCost` reads back.
 *
 * Callers additionally assume it is the cost's **only** exile atom: `exileChoices` is a single
 * flat channel shared by every exile atom in a composite cost, so a cost pairing this atom with
 * an `ExileFrom` would need the two selections split per atom before either could be trusted.
 * No printed card has that shape; a card that does must fix the channel, not the call site.
 */
internal fun AbilityCost.firstExileForTotalAtomOrNull(): CostAtom.ExileFromGraveyardForTotal? =
    when (this) {
        is AbilityCost.Atom -> atom as? CostAtom.ExileFromGraveyardForTotal
        is AbilityCost.Composite -> costs.firstNotNullOfOrNull { it.firstExileForTotalAtomOrNull() }
        else -> null
    }

/**
 * Whether this cost exiles anything at all — either the sum-gated
 * [CostAtom.ExileFromGraveyardForTotal] or a plain counted [CostAtom.ExileFrom].
 *
 * Both feed the same flat `exileChoices` channel, and both produce cards the resolving effect
 * may need to name via `CardSource.ExiledAsCost` — Necropolis reads the mana value of the very
 * creature card its "Exile a creature card from your graveyard:" cost just exiled. Gating the
 * record on the *sum-gated* atom alone left the plain counted form with an empty list, so an
 * effect reading it back saw nothing.
 */
internal fun AbilityCost.hasExileAtom(): Boolean = when (this) {
    is AbilityCost.Atom -> atom is CostAtom.ExileFrom || atom is CostAtom.ExileFromGraveyardForTotal
    is AbilityCost.Composite -> costs.any { it.hasExileAtom() }
    else -> false
}

/**
 * Whether the given ability cost includes a Tap sub-cost.
 * The source of a Tap-cost ability cannot also serve as a mana source during payment.
 */
internal fun AbilityCost.hasTapCost(): Boolean = when (this) {
    is AbilityCost.Tap -> true
    is AbilityCost.Composite -> costs.any { it is AbilityCost.Tap }
    else -> false
}

/**
 * Whether this cost includes the tap symbol **or the untap symbol** — the CR 302.6
 * summoning-sickness gate for a creature's activated ability.
 */
internal fun AbilityCost.touchesTapSymbol(): Boolean {
    val costTouchesTapSymbol = { c: AbilityCost -> c is AbilityCost.Tap || c is AbilityCost.Untap }
    return costTouchesTapSymbol(this) ||
        (this is AbilityCost.Composite && costs.any(costTouchesTapSymbol))
}

/** Whether this cost taps the creature the source is attached to (bare or inside a composite). */
internal fun AbilityCost.hasTapAttachedCreatureCost(): Boolean =
    this is AbilityCost.TapAttachedCreature ||
        (this is AbilityCost.Composite && costs.any { it is AbilityCost.TapAttachedCreature })

/**
 * Whether this cost removes the source from its current zone — a self-exile, self-sacrifice, or
 * self-bounce. Used to decide whether to snapshot the source's counters before payment so the
 * resolving effect can read the pre-cost count (DynamicAmount.LastKnownSourceCounters).
 */
internal fun AbilityCost.exilesOrSacrificesSelf(): Boolean = when (this) {
    is AbilityCost.ExileSelf, is AbilityCost.SacrificeSelf, is AbilityCost.ReturnSelfToHand -> true
    is AbilityCost.Composite -> costs.any { it.exilesOrSacrificesSelf() }
    else -> false
}

/**
 * Whether this cost includes "Reveal the creature type you chose" — the signal to capture the
 * source's noted type before the cost is paid. The same activation typically sacrifices the
 * source (A Killer Among Us), so by resolution the permanent and its note are gone; this is
 * the CR 113.7a capture that keeps the ability's "if the target is the chosen type" answerable.
 */
internal fun AbilityCost.revealsNotedCreatureType(): Boolean = when (this) {
    is AbilityCost.Atom -> atom is CostAtom.RevealNotedCreatureType
    is AbilityCost.Composite -> costs.any { it.revealsNotedCreatureType() }
    else -> false
}

/**
 * Extract the ManaCost from an ability cost, if present.
 */
internal fun AbilityCost.extractManaCost(): ManaCost? = when (this) {
    is AbilityCost.Atom -> manaCostOrNull
    is AbilityCost.Composite -> costs.firstNotNullOfOrNull { it.manaCostOrNull }
    else -> null
}

/**
 * This cost with its mana portion replaced by [manaCost] — used after convoke/waterbend reduced
 * the mana half, so the rest of the activation prices and pays the reduced amount.
 */
internal fun AbilityCost.withManaPortion(manaCost: ManaCost): AbilityCost = when (this) {
    is AbilityCost.Atom -> AbilityCost.Atom(CostAtom.Mana(manaCost))
    is AbilityCost.Composite -> AbilityCost.Composite(costs.map { subCost ->
        if (subCost.manaCostOrNull != null) AbilityCost.Atom(CostAtom.Mana(manaCost)) else subCost
    })
    else -> this
}

/** Lower the generic part of the first mana portion of this cost by [amount]. */
internal fun AbilityCost.reduceGenericMana(amount: Int): AbilityCost = when (this) {
    is AbilityCost.Atom -> manaCostOrNull
        ?.let { AbilityCost.Atom(CostAtom.Mana(it.reduceGeneric(amount))) } ?: this
    is AbilityCost.Composite -> {
        var applied = false
        AbilityCost.Composite(costs.map { sub ->
            val subMana = sub.manaCostOrNull
            if (!applied && subMana != null) {
                applied = true
                AbilityCost.Atom(CostAtom.Mana(subMana.reduceGeneric(amount)))
            } else sub
        })
    }
    else -> this
}

/**
 * Strip the Mana portion from an ability cost — used when Explicit payment already
 * tapped the required sources, so the mana pool deduction should be skipped.
 */
internal fun AbilityCost.stripManaCost(): AbilityCost = when (this) {
    is AbilityCost.Atom -> if (manaCostOrNull != null) AbilityCost.Free else this
    is AbilityCost.Composite -> {
        val nonManaCosts = costs.filter { it.manaCostOrNull == null }
        when (nonManaCosts.size) {
            0 -> AbilityCost.Free
            1 -> nonManaCosts.single()
            else -> AbilityCost.Composite(nonManaCosts)
        }
    }
    else -> this
}

/**
 * Pull the [AbilityCost.TapXPermanents] sub-cost out of an ability cost (top-level or
 * inside a [AbilityCost.Composite]), or null if none. Used by the legal-actions submission
 * path to detect that an activation needs to pause for an X choice + tap-target selection.
 */
internal fun AbilityCost.extractTapXPermanentsCost(): AbilityCost.TapXPermanents? = when (this) {
    is AbilityCost.TapXPermanents -> this
    is AbilityCost.Composite -> costs.filterIsInstance<AbilityCost.TapXPermanents>().firstOrNull()
    else -> null
}

/**
 * Pull the graveyard-exile [CostAtom.ExileFrom] sub-cost out of an ability cost (top-level or
 * inside a [AbilityCost.Composite]), or null if none. Used by the legal-actions submission
 * path to detect that an activation needs to pause for a card-selection decision when the
 * player has more matching graveyard cards than the cost requires.
 */
internal fun AbilityCost.extractExileFromGraveyardCost(): CostAtom.ExileFrom? = when (this) {
    is AbilityCost.Atom -> (atom as? CostAtom.ExileFrom)?.takeIf { it.zone == Zone.GRAVEYARD }
    is AbilityCost.Composite -> costs.firstNotNullOfOrNull {
        ((it as? AbilityCost.Atom)?.atom as? CostAtom.ExileFrom)?.takeIf { ex -> ex.zone == Zone.GRAVEYARD }
    }
    else -> null
}

/**
 * Pull the [AbilityCost.ExileXFromGraveyard] sub-cost out of an ability cost, or null if none.
 * Used by the legal-actions submission path to bind X (Winter, Cursed Rider — X with no `{X}`
 * mana symbol) and to pause for *which* graveyard cards the player exiles.
 */
internal fun AbilityCost.extractExileXFromGraveyardCost(): AbilityCost.ExileXFromGraveyard? =
    when (this) {
        is AbilityCost.ExileXFromGraveyard -> this
        is AbilityCost.Composite -> costs.filterIsInstance<AbilityCost.ExileXFromGraveyard>().firstOrNull()
        else -> null
    }

/**
 * Pull the [CostAtom.Sacrifice] sub-cost out of an ability cost (top-level [AbilityCost.Atom] or
 * inside a [AbilityCost.Composite]), or null if none. Used by the legal-actions submission path
 * to detect that an activation needs to pause for a sacrifice-target selection when the player
 * controls more matching permanents than the cost requires (Sage of Lat-Nam, Atog, …).
 */
/** The put-from-hand-on-library atom in this cost (Leashling), top-level or in a Composite. */
internal fun AbilityCost.extractPutOnLibraryCost(): CostAtom.PutFromHandOnTopOfLibrary? = when (this) {
    is AbilityCost.Atom -> atom as? CostAtom.PutFromHandOnTopOfLibrary
    is AbilityCost.Composite -> costs.firstNotNullOfOrNull {
        (it as? AbilityCost.Atom)?.atom as? CostAtom.PutFromHandOnTopOfLibrary
    }
    else -> null
}

internal fun AbilityCost.extractSacrificeCost(): CostAtom.Sacrifice? = when (this) {
    is AbilityCost.Atom -> atom as? CostAtom.Sacrifice
    is AbilityCost.Composite -> costs.firstNotNullOfOrNull {
        (it as? AbilityCost.Atom)?.atom as? CostAtom.Sacrifice
    }
    else -> null
}

internal fun AbilityCost.extractSacrificeAllCost(): CostAtom.SacrificeAll? = when (this) {
    is AbilityCost.Atom -> atom as? CostAtom.SacrificeAll
    is AbilityCost.Composite -> costs.firstNotNullOfOrNull {
        (it as? AbilityCost.Atom)?.atom as? CostAtom.SacrificeAll
    }
    else -> null
}

/**
 * Pull the [CostAtom.VariablePermanents] variable-count sub-cost out of an ability cost, or null if
 * none. Drives the two-step activation flow for "Exile one or more other [filter] you control
 * with total mana value X" costs (Fabrication Foundry): the handler pauses to let the player pick
 * which permanents to exile, then — because the target's legality depends on the resulting X —
 * pauses again for the target choice.
 */
internal fun AbilityCost.extractVariablePermanentsCost(): CostAtom.VariablePermanents? = when (this) {
    is AbilityCost.Atom -> atom as? CostAtom.VariablePermanents
    is AbilityCost.Composite -> costs.firstNotNullOfOrNull {
        (it as? AbilityCost.Atom)?.atom as? CostAtom.VariablePermanents
    }
    else -> null
}
