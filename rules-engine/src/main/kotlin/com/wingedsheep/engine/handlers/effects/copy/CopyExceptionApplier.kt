package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * The one place that turns a [CopyExceptions] into an actual modification of copied characteristics
 * (CR 707.9 — "the copy effect may specify that the copy has certain values").
 *
 * The copy paths that route through here:
 *
 *  - [com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor]
 *    — an existing permanent becomes a copy (Mirrorform, Fleeting Reflection, Absorbing Man).
 *  - [com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfTargetExecutor] — a token copy
 *    of a targeted permanent is minted (Molten Duplication, Ardyn, Shelob).
 *  - [com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfSourceExecutor] — a token copy
 *    of the ability's own source (Vaultborn Tyrant, Ran and Shaw).
 *  - [com.wingedsheep.engine.handlers.continuations.ModalAndCloneContinuationResumer]'s
 *    `EntersAsCopy` path — a permanent or spell entering as a copy (Clone, Sakashima, Mockingbird).
 *
 * They used to carry independent implementations of the same type-line arithmetic, which is why
 * "except it isn't legendary" and "except it's a creature in addition to its other types" worked on
 * tokens and silently did nothing on permanents. Keeping the arithmetic here means a new exception
 * is written once and every path above gets it.
 *
 * Not (yet) routed through here: the two paths whose only "except" clause is the single boolean
 * `removeLegendary` and which therefore have no arithmetic to share —
 * [com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfEquippedCreatureExecutor]
 * (Helm of the Host) and
 * [com.wingedsheep.engine.handlers.effects.stack.StormCopyEffectExecutor] (copies of *spells*,
 * CR 707.10, where the copy lives on the stack rather than as a permanent's copiable values).
 *
 * Copiable values only: everything applied here lives on the [CardComponent], so it is itself
 * copiable (a later copy of the copy sees it) and it lasts exactly as long as the copy does.
 * Counters, tapped state, attachments and non-copy continuous effects are untouched — they live on
 * other components.
 */
object CopyExceptionApplier {

    /**
     * The type line a copy of [base] ends up with. Split out so callers that need to know what the
     * copy *will* be before building it (the Aura-token host choice, which must know whether the
     * copy is still an Aura) ask the same question the applier answers.
     *
     * Order matters on the supertype axis: additions first, then removals, so a supertype named in
     * both is removed — "it's legendary" and "it isn't legendary" can't both be true, and the
     * removal is the more specific clause.
     *
     * Card types and subtypes behave the same way as each other: an override is a clause that states
     * the whole type line (CR 205.1a), so it replaces the copied types *and* supersedes an addition
     * on the same axis — an "in addition to its other types" clause (CR 205.1b) and a replacing one
     * can't both be printed on the same characteristic.
     */
    fun typeLine(base: TypeLine, exceptions: CopyExceptions): TypeLine = base.copy(
        supertypes = base.supertypes + exceptions.addedSupertypes - exceptions.removedSupertypes,
        cardTypes = exceptions.overrideCardTypes ?: (base.cardTypes + exceptions.addedCardTypes),
        subtypes = exceptions.overrideSubtypes ?: (base.subtypes + exceptions.addedSubtypes),
    )

    /**
     * Apply [exceptions] to the copied card characteristics in [base], returning the
     * [CardComponent] the copy should present. [base] is the copy *source's* component; the caller
     * is responsible for anything that isn't a copiable characteristic (owner, controller, token
     * marker, revert markers).
     *
     * A no-op fast path when the exceptions are empty, so a plain copy allocates nothing.
     */
    fun apply(base: CardComponent, exceptions: CopyExceptions): CardComponent {
        if (exceptions.isEmpty) return base
        return base.copy(
            name = exceptions.nameOverride ?: base.name,
            typeLine = typeLine(base.typeLine, exceptions),
            baseStats = baseStats(base.baseStats, exceptions),
            baseKeywords = base.baseKeywords + exceptions.addedKeywords,
            colors = exceptions.overrideColors ?: (base.colors + exceptions.addedColors),
            // "…and it has no mana cost" (Embalm / Eternalize, CR 702.128a) — mana value 0, and
            // that 0 is itself a copiable value.
            manaCost = if (exceptions.noManaCost) ManaCost.ZERO else base.manaCost,
        )
    }

    /**
     * Base P/T after a "he's a 4/4" clause. Unlike a plain `copy(power = …)` this *creates* base
     * stats when the copied object had none: Absorbing Man copying a land becomes "a legendary 4/4
     * Human Villain creature in addition to its other types", so the 4/4 has to come from nowhere.
     * A half-specified override keeps the copied value on the other half.
     */
    private fun baseStats(base: CreatureStats?, exceptions: CopyExceptions): CreatureStats? {
        val power = exceptions.powerOverride
        val toughness = exceptions.toughnessOverride
        if (power == null && toughness == null) return base
        return CreatureStats(
            power = power?.let { CharacteristicValue.Fixed(it) }
                ?: base?.power
                ?: CharacteristicValue.Fixed(0),
            toughness = toughness?.let { CharacteristicValue.Fixed(it) }
                ?: base?.toughness
                ?: CharacteristicValue.Fixed(0),
        )
    }
}
