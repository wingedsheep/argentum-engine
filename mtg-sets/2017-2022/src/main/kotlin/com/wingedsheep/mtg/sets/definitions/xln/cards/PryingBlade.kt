package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Prying Blade
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/+0.
 * Whenever equipped creature deals combat damage to a player, create a Treasure token.
 * Equip {2}
 */
val PryingBlade = card("Prying Blade") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0.\n" +
        "Whenever equipped creature deals combat damage to a player, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTreasure()
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "244"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad45720e-2870-414d-8119-8e48c5600d3b.jpg?1783935708"
    }
}
