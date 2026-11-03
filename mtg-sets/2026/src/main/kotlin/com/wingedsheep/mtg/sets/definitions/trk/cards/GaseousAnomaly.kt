package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gaseous Anomaly
 * Land
 *
 * {T}: Add {C}.
 * {5}{U}, {T}, Sacrifice this land: Draw two cards. Create a tapped Planet land token named New
 * Planet with "{T}: Add one mana of any color."
 */
val GaseousAnomaly = card("Gaseous Anomaly") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{5}{U}, {T}, Sacrifice this land: Draw two cards. Create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{U}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DrawCards(2) then Effects.CreateNewPlanet()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "284"
        artist = "Racrufi"
        flavorText = "\"It's like the warp engines are feeding it!\"\n—Lieutenant Hikaru Sulu"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6974579-7e88-4d7f-9b05-93d08167cb17.jpg?1793361441"
    }
}
