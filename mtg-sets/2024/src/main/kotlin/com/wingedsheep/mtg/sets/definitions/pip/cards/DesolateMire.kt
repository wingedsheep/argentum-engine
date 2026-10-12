package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

val DesolateMire = card("Desolate Mire") {
    manaCost = ""
    colorIdentity = "BW"
    typeLine = "Land"
    oracleText = "{1}, {T}: Add {W}{B}."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.WHITE) then Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Tomáš Honz"
        flavorText = "The region known to survivors as \"Toxic Valley\" is covered by industrial white powder and polluted water sources. The lake and other deadly waters of the Toxic Valley region are home to all manner of mutated, aquatic beasts."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05f38637-8cdc-42bb-b164-85596b6b14b9.jpg?1783912374"
    }
}
