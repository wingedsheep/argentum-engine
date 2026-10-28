package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ardent Electromancer — Zendikar Rising #135
 * {2}{R} · Creature — Human Wizard · 3/2
 *
 * When this creature enters, add {R} for each creature in your party.
 */
val ArdentElectromancer = card("Ardent Electromancer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, add {R} for each creature in your party. " +
        "(Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddMana(Color.RED, DynamicAmounts.partySize())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Lie Setiawan"
        flavorText = "\"Trust me, we won't need torches.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8685c8e-935f-49f2-acf2-8e3c41c5b330.jpg?1783929361"
    }
}
