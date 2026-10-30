package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Giant's Skewer
 * {1}{B}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+1.
 * Whenever equipped creature deals combat damage to a creature, create a Food token.
 * Equip {3}
 */
val GiantsSkewer = card("Giant's Skewer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+1.\n" +
        "Whenever equipped creature deals combat damage to a creature, create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+2, +1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.CreateFood()
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/050c03f9-ccbd-4dcc-9789-8013875ef470.jpg?1783932639"
    }
}
