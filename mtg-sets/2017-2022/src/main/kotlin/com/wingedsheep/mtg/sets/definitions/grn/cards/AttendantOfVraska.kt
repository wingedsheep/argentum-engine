package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Attendant of Vraska
 * {1}{B}{G}
 * Creature — Zombie Soldier
 * 3/3
 * When this creature dies, if you control a Vraska planeswalker, you gain life equal to this creature's power.
 *
 * "This creature's power" reads its last-known power via [DynamicAmounts.sourcePower].
 */
val AttendantOfVraska = card("Attendant of Vraska") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Zombie Soldier"
    oracleText = "When this creature dies, if you control a Vraska planeswalker, you gain life equal to this creature's power."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dies()
        interveningIf = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Vraska"))
        effect = Effects.GainLife(DynamicAmounts.sourcePower())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "271"
        artist = "Magali Villeneuve"
        flavorText = "When the Erstwhile were unearthed from their tombs, they became a ready-made army for Vraska."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f4840f1-3db3-4ba6-b75b-bbd87251a3af.jpg?1783934093"
    }
}
