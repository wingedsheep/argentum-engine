package com.wingedsheep.mtg.sets.definitions.plc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Simian Spirit Guide
 * {2}{R}
 * Creature — Ape Spirit
 * 2/2
 * Exile this card from your hand: Add {R}.
 *
 * A mana ability that functions from the hand (CR 605.1a: it doesn't target and could add mana).
 */
val SimianSpiritGuide = card("Simian Spirit Guide") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ape Spirit"
    oracleText = "Exile this card from your hand: Add {R}."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.ExileSelf
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
        activateFromZone = Zone.HAND
        description = "Exile this card from your hand: Add {R}."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Dave DeVries"
        flavorText = "\"All my spells smell like burnt hair lately.\"\n—Jaya Ballard, task mage"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d7f701c-dcdc-4067-8d00-b4b7aadee9ba.jpg?1783943142"
    }
}
