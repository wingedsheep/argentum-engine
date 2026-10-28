package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cascade Seer — Zendikar Rising #48
 * {3}{U} · Creature — Merfolk Wizard · 3/3
 *
 * When this creature enters, scry X, where X is the number of creatures in your party.
 */
val CascadeSeer = card("Cascade Seer") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "When this creature enters, scry X, where X is the number of creatures in your party. " +
        "(Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.scry(DynamicAmounts.partySize())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Brian Valeza"
        flavorText = "\"Perhaps you can't see the path, but that doesn't mean it isn't there.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13105c88-2926-40ad-bc7a-09d515aa36c6.jpg?1783929404"
    }
}
