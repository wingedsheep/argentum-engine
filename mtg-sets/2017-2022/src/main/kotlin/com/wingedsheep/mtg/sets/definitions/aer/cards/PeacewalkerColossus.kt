package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val PeacewalkerColossus = card("Peacewalker Colossus") {
    manaCost = "{3}"
    colorIdentity = "W"
    typeLine = "Artifact — Vehicle"
    power = 6
    toughness = 6
    oracleText = "{1}{W}: Another target Vehicle you control becomes an artifact creature until end of turn.\nCrew 4 (Tap any number of creatures you control with total power 4 or more: This Vehicle becomes an artifact creature until end of turn.)"

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val vehicle = target(TargetFilter(
            GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl(),
            excludeSelf = true
        ))
        // Add types without changing printed or previously set base power and toughness.
        effect = Effects.AddCardType("Artifact", vehicle, Duration.EndOfTurn) then
            Effects.AddCardType("Creature", vehicle, Duration.EndOfTurn)
        description = "Another target Vehicle you control becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Vincent Proce"
        flavorText = "\"Peace? Its sole purpose is fear.\"\n—Saheeli Rai"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1599b545-6b8e-4350-980a-59349374400d.jpg?1783936720"
    }
}
