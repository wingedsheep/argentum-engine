package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Persistent Specimen
 * {B}
 * Creature — Skeleton
 * 1/1
 * {2}{B}: Return this card from your graveyard to the battlefield tapped.
 */
val PersistentSpecimen = card("Persistent Specimen") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton"
    oracleText = "{2}{B}: Return this card from your graveyard to the battlefield tapped."
    power = 1
    toughness = 1
    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Scott Murphy"
        flavorText = "\"The jaw bone's connected to the skull bone. The skull bone's connected to the . . . uh . . . hook thingy.\"\n—Garl, sticher's assistant"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7baf973-3202-4fea-8861-a4a5ec228640.jpg?1782703101"
    }
}
