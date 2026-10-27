package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gilded Assault Cart
 * {1}{R}{R}
 * Artifact — Vehicle
 * 5/1
 * Trample
 * Crew 2
 * Sacrifice two Treasures: Return this card from your graveyard to your hand.
 *
 * The recursion is a graveyard-activated ability (`activateFromZone = Zone.GRAVEYARD`) whose
 * cost sacrifices two Treasures from the battlefield and whose effect moves the card itself home.
 */
val GildedAssaultCart = card("Gilded Assault Cart") {
    manaCost = "{1}{R}{R}"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 1
    oracleText = "Trample\nCrew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes an artifact creature until end of turn.)\nSacrifice two Treasures: Return this card from your graveyard to your hand."

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.crew(2))

    activatedAbility {
        cost = Costs.SacrificeMultiple(2, GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE))
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "390"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5274c2fa-eb0e-438b-8a48-d0b58829083a.jpg?1783928116"
    }
}
