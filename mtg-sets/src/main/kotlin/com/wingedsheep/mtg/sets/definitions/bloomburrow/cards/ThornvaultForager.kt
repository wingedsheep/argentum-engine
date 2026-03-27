package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thornvault Forager
 * {1}{G}
 * Creature — Squirrel Ranger
 * 2/2
 *
 * {T}: Add {G}.
 * {T}, Forage: Add two mana in any combination of colors.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 * {3}{G}, {T}: Search your library for a Squirrel card, reveal it, put it into your hand, then shuffle.
 */
val ThornvaultForager = card("Thornvault Forager") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Squirrel Ranger"
    power = 2
    toughness = 2
    oracleText = "{T}: Add {G}.\n{T}, Forage: Add two mana in any combination of colors. (To forage, exile three cards from your graveyard or sacrifice a Food.)\n{3}{G}, {T}: Search your library for a Squirrel card, reveal it, put it into your hand, then shuffle."

    // {T}: Add {G}
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
    }

    // {T}, Forage: Add two mana in any combination of colors
    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Forage())
        effect = Effects.AddAnyColorMana(2)
        manaAbility = true
    }

    // {3}{G}, {T}: Search your library for a Squirrel card, reveal it, put it into your hand, then shuffle
    activatedAbility {
        cost = Costs.Composite(Costs.Mana(ManaCost.parse("{3}{G}")), Costs.Tap)
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter.Creature.withSubtype("Squirrel"),
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c2d6b02-a453-40f9-992a-5c5542987cfb.jpg?1721933896"
    }
}
