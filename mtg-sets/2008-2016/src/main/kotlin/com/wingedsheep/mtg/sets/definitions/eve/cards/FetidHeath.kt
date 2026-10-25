package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Fetid Heath
 * Land
 * {T}: Add {C}.
 * {W/B}, {T}: Add {W}{W}, {W}{B}, or {B}{B}.
 */
val FetidHeath = card("Fetid Heath") {
    manaCost = ""
    colorIdentity = "WB"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{W/B}, {T}: Add {W}{W}, {W}{B}, or {B}{B}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W/B}"), Costs.Tap)
        effect = Effects.AddMana(Color.WHITE, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W/B}"), Costs.Tap)
        effect = Effects.AddMana(Color.WHITE) then Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W/B}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Daarken"
        flavorText = "\"Do not linger in such places, child. There the gwyllions dance. If they find you, " +
            "you will join in their revels for eternity.\"\n—Talara, elvish safewright"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fbb9790-3744-4dcb-881a-452573298822.jpg?1783942654"
    }
}
