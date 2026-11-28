package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Nephalia Drownyard
 * (no mana cost)
 * Land
 * {T}: Add {C}.
 * {1}{U}{B}, {T}: Target player mills three cards.
 */
val NephaliaDrownyard = card("Nephalia Drownyard") {
    manaCost = ""
    colorIdentity = "BU"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{1}{U}{B}, {T}: Target player mills three cards."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}{B}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Cliff Childs"
        flavorText = "\"Some see a coastal disaster. I see a fully-stocked seagraf.\"\n—Jadar, ghoulcaller of Nephalia"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef058312-6926-49f8-ae72-a8d60fedbf6c.jpg"
    }
}
