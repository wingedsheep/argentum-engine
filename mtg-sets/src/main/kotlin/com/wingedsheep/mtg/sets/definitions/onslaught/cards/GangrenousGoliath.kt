package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Gangrenous Goliath
 * {3}{B}{B}
 * Creature — Zombie Giant
 * 4/4
 * Tap three untapped Clerics you control: Return Gangrenous Goliath from your graveyard to your hand.
 */
val GangrenousGoliath = card("Gangrenous Goliath") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Zombie Giant"
    power = 4
    toughness = 4
    oracleText = "Tap three untapped Clerics you control: Return Gangrenous Goliath from your graveyard to your hand."

    activatedAbility {
        cost = Costs.TapPermanents(3, GameObjectFilter.Creature.withSubtype("Cleric"))
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Justin Sweet"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/6/9/69b58b6b-24cd-4440-b99c-d88d44b3c41c.jpg?1562919881"
    }
}
