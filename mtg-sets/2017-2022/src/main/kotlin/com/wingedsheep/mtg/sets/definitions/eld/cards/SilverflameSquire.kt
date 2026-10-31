package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silverflame Squire // On Alert
 * {1}{W}
 * Creature — Human Soldier
 * 2/1
 *
 * Adventure: On Alert — {2}{W}, Instant — Adventure
 * Target creature gets +2/+2 until end of turn. Untap it.
 */
val SilverflameSquire = card("Silverflame Squire") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = ""

    adventure("On Alert") {
        manaCost = "{2}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Target creature gets +2/+2 until end of turn. Untap it. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 2, t) then Effects.Untap(t)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Lie Setiawan"
        flavorText = "Beacons along the borderlands are lit at any incursion from the wilds."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bd105f3-fa33-4490-aea9-b47ca121b664.jpg?1783932665"
    }
}
