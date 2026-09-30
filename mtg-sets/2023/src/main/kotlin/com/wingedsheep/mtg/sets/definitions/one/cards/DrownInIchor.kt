package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Drown in Ichor
 * {1}{B}
 * Sorcery
 * Target creature gets -4/-4 until end of turn. Proliferate.
 *
 * Both happen during resolution, before state-based actions are checked, so proliferating a
 * counter onto the shrunken creature can still save it.
 */
val DrownInIchor = card("Drown in Ichor") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target creature gets -4/-4 until end of turn. Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-4, -4, t) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Tiffany Turrill"
        flavorText = "Sinking into the Dross is neither a gentle death nor a quiet one. But once the necrogen takes hold, it is inevitable."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf148ef4-0ed7-4fbe-8ab6-01dd3981c8d3.jpg?1783918049"
    }
}
