package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gilded Lotus — Mirrodin #175
 * {5} · Artifact
 *
 * {T}: Add three mana of any one color.
 *
 * "Any *one* color" (not "any combination"): the player picks a single colour and gets three of
 * it, which is [Effects.AddAnyColorMana] — the facade over `AddManaOfChoiceEffect` with
 * `ManaColorSet.AnyColor` and a fixed amount. `AddManaInAnyCombination` would be wrong here; it
 * colours each mana independently.
 *
 * Marked `manaAbility = true` with [TimingRule.ManaAbility]: no target, adds mana, isn't a loyalty
 * ability (CR 605.1a), so it never uses the stack and can be activated while paying a cost.
 *
 * Mirrodin is the earliest printing, so the canonical definition belongs here. It previously lived
 * in Dominaria (2018), which is now a `Printing(...)` row like M13, BLC and FDN already were.
 */
val GildedLotus = card("Gilded Lotus") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add three mana of any one color."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add three mana of any one color."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Martina Pilcerova"
        flavorText = "Over such beauty, wars are fought. With such power, wars are won."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1d5e4c8-dfd0-45bc-8000-ebfaccfefec3.jpg?1783944521"
    }
}
