package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.flurry
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Taigam, Master Opportunist — Tarkir: Dragonstorm #60
 * {1}{U} · Legendary Creature — Human Monk · 2/2
 *
 * Flurry — Whenever you cast your second spell each turn, copy it, then exile the spell you
 * cast with four time counters on it. If it doesn't have suspend, it gains suspend. (At the
 * beginning of its owner's upkeep, they remove a time counter. When the last is removed,
 * they may play it without paying its mana cost. If it's a creature, it has haste.)
 *
 * Composed from atomic primitives rather than a bespoke executor:
 *
 *   1. **Copy it** — [Effects.CopyTargetSpell] on [EffectTarget.TriggeringEntity] (the spell
 *      you cast, exactly as Alania, Divergent Storm copies its triggering spell). The copy is
 *      created on the stack and resolves normally.
 *   2. **Exile the spell you cast** — a [CounterEffect] to [CounterDestination.Exile] removes
 *      the original spell from the stack into its owner's exile (a spell can't be lifted off
 *      the stack with a zone-move effect; it must be countered/exiled).
 *   3. **Give it suspend** — [Effects.Suspend] puts four time counters on the now-exiled card
 *      and marks it suspended. The owner's-upkeep countdown that plays it for free is supplied
 *      by the engine off that marker, so no card-specific cast logic lives here (see
 *      [com.wingedsheep.sdk.scripting.Suspend]).
 *
 * "If it doesn't have suspend, it gains suspend" — Taigam only ever exiles freshly-cast
 * spells, none of which have suspend, so the conditional is always the grant.
 */
val TaigamMasterOpportunist = card("Taigam, Master Opportunist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Monk"
    power = 2
    toughness = 2
    oracleText = "Flurry — Whenever you cast your second spell each turn, copy it, then exile " +
        "the spell you cast with four time counters on it. If it doesn't have suspend, it " +
        "gains suspend. (At the beginning of its owner's upkeep, they remove a time counter. " +
        "When the last is removed, they may play it without paying its mana cost. If it's a " +
        "creature, it has haste.)"

    flurry {
        description = "copy it, then exile the spell you cast with four time counters on it. " +
            "If it doesn't have suspend, it gains suspend"
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity, mayChooseNewTargets = false) then
            Effects.CounterTriggeringSpell(
                counterDestination = CounterDestination.Exile(),
            ) then
            Effects.Suspend(EffectTarget.TriggeringEntity, timeCounters = 4)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "60"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8693d631-05f6-414d-9e49-6385746e8960.jpg?1743204200"
    }
}
