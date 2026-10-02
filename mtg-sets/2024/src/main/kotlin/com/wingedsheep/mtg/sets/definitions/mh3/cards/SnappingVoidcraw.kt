package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

val SnappingVoidcraw = card("Snapping Voidcraw") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Eldrazi Turtle"
    power = 1
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n{T}: Add {C}{C}.\n{3}{C}, {T}: Draw a card."

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{C}"), Costs.Tap)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Camille Alquier"
        flavorText = "Once considered a delicacy, turtle soup is no longer served in Nephalia's inns."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9185371c-2dde-48ad-ab27-08be04b3c522.jpg?1783911245"
    }
}
