package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Infested Fleshcutter
 * {1}{W}
 * Artifact — Equipment
 * Equipped creature gets +2/+0.
 * Whenever equipped creature attacks, create a 1/1 colorless Phyrexian Mite artifact creature token
 * with toxic 1 and "This token can't block."
 * Equip {2}{W}
 *
 * The attack trigger lives on the Equipment (`Triggers.attached.attacks()` = "equipped creature
 * attacks"), the same shell as Spiked Ripsaw; the Mite is the predefined ONE token.
 */
val InfestedFleshcutter = card("Infested Fleshcutter") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "Whenever equipped creature attacks, create a 1/1 colorless Phyrexian Mite artifact creature " +
        "token with toxic 1 and \"This token can't block.\" (Players dealt combat damage by it also " +
        "get a poison counter.)\n" +
        "Equip {2}{W}"

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.CreatePhyrexianMite(1)
        description = "Whenever equipped creature attacks, create a 1/1 colorless Phyrexian Mite " +
            "artifact creature token with toxic 1 and \"This token can't block.\""
    }

    equipAbility("{2}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f03d6bf-cbba-4ad7-8cec-065a47f03dbe.jpg?1783918079"
    }
}
