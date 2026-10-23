package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crossroads Candleguide
 * {4}
 * Artifact Creature — Scarecrow
 * 3/4
 * When this creature enters, exile up to one target card from a graveyard.
 * {2}: Add one mana of any color.
 */
val CrossroadsCandleguide = card("Crossroads Candleguide") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Scarecrow"
    oracleText = "When this creature enters, exile up to one target card from a graveyard.\n" +
        "{2}: Add one mana of any color."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Move(t, Zone.EXILE)
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "253"
        artist = "Dan Murayama Scott"
        flavorText = "Do the candleguides' heads turn to follow you as you pass, or is it merely a trick of the flickering light?"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cad1524-9e75-47e0-b817-47110634802a.jpg"
    }
}
