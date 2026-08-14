package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Trolls of Tel-Jilad — Mirrodin #136
 * {5}{G}{G} · Creature — Troll Shaman · 5/6
 *
 * {1}{G}: Regenerate target green creature.
 *
 * Modelling notes:
 * - The ability targets *any* green creature, not just this one and not just creatures you
 *   control, so the filter is a bare colour filter ([TargetFilter.Creature] narrowed with
 *   [Color.GREEN]) with no controller restriction. The Trolls are themselves green, so
 *   "regenerate itself" is just the self-targeting case of the same ability.
 * - Colour is read off projected state, so a creature that has been *made* green by another
 *   effect is a legal target and one that has lost green is not — the target is re-checked on
 *   resolution and the ability fizzles if it no longer matches.
 * - [RegenerateEffect] applies a shield lasting until end of turn (CR 701.15); it is not a
 *   regeneration ability granted to the creature, so "can't be regenerated" markers still win.
 */
val TrollsOfTelJilad = card("Trolls of Tel-Jilad") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll Shaman"
    power = 5
    toughness = 6
    oracleText = "{1}{G}: Regenerate target green creature."

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        val greenCreature = target(
            "target green creature",
            TargetCreature(filter = TargetFilter.Creature.withColor(Color.GREEN))
        )
        effect = RegenerateEffect(greenCreature)
        description = "{1}{G}: Regenerate target green creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Marcelo Vignali"
        flavorText = "\"The secret of this world weighs upon us, and we have been shaped by " +
            "time and duty to bear it.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6535f2ad-6cda-4be9-8e4f-6f062b63be31.jpg?1783944530"
    }
}
