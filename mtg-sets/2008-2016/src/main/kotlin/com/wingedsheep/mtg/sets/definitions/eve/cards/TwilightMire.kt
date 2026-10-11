package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Twilight Mire
 * Land
 * {T}: Add {C}.
 * {B/G}, {T}: Add {B}{B}, {B}{G}, or {G}{G}.
 */
val TwilightMire = card("Twilight Mire") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{B/G}, {T}: Add {B}{B}, {B}{G}, or {G}{G}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B/G}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B/G}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK) then Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B/G}"), Costs.Tap)
        effect = Effects.AddMana(Color.GREEN, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Rob Alexander"
        flavorText = "As their roots soaked in a weak tea of bogwater and decay, the trees absorbed the essence of death into their living tissue."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff619a09-4fe5-4341-9077-88a0a9f4fbf9.jpg"
    }
}
