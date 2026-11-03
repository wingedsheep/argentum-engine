package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Tricorder
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature gets +1/+1.
 * Whenever equipped creature attacks, surveil 1.
 * Equip {1}
 */
val Tricorder = card("Tricorder") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature attacks, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.Surveil(1)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "270"
        artist = "Tony Foti"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c872f02-80ac-49e3-a777-3526d35ad7cc.jpg?1792508457"
    }
}
