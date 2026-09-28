package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Trailblazing Historian
 * {1}{R}
 * Creature — Human Shaman
 * 1/3
 * Haste
 * {T}: Another target creature gains haste until end of turn.
 */
val TrailblazingHistorian = card("Trailblazing Historian") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Haste\n{T}: Another target creature gains haste until end of turn."
    power = 1
    toughness = 3

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Jason A. Engle"
        flavorText = "Spells once used to traverse Lorehold excavation sites were swiftly repurposed to keep the students one step ahead of the Invasion Tree's grasp."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a53088da-25b5-4c7e-9a11-456fbc814dfc.jpg?1783916980"
    }
}
