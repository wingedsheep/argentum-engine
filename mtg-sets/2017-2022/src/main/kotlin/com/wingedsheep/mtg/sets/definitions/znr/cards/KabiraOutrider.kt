package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kabira Outrider — Zendikar Rising #18
 * {3}{W} · Creature — Human Warrior · 3/3
 *
 * When this creature enters, target creature gets +1/+1 until end of turn for each creature in
 * your party.
 */
val KabiraOutrider = card("Kabira Outrider") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    oracleText = "When this creature enters, target creature gets +1/+1 until end of turn for each creature " +
        "in your party. (Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.partySize(), DynamicAmounts.partySize(), creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Izzy"
        flavorText = "Ondu moas make wildly uncomfortable mounts, but for determined warriors, their speed is enough to make up for it."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53c81719-f820-4674-9cfb-cd5eaf0c1f0d.jpg?1783929418"
    }
}
