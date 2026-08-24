package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Icatian Lieutenant
 * {W}{W}
 * Creature — Human Soldier
 * 1/2
 * {1}{W}: Target Soldier creature gets +1/+0 until end of turn.
 */
val IcatianLieutenant = card("Icatian Lieutenant") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "{1}{W}: Target Soldier creature gets +1/+0 until end of turn."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val t = target(
            "target Soldier creature",
            TargetCreature(filter = TargetFilter.Creature.withSubtype(Subtype.SOLDIER))
        )
        effect = Effects.ModifyStats(1, 0, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Pete Venters"
        flavorText = "To become an officer, an Icatian Soldier had to pass a series of tests. These evaluated not only fighting and leadership skills, but also integrity, honor, and moral strength."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39fec59a-4ade-4c6f-ae7d-911fbe6da26d.jpg?1783947917"
    }
}
