package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Shard of Broken Glass
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature gets +1/+0.
 * Whenever equipped creature attacks, you may mill two cards.
 * Equip {1}
 *
 * Sword of the Animist's `Triggers.attached.attacks()` shape carrying an optional
 * `Patterns.Library.mill(2)` for the Equipment's controller.
 */
val ShardOfBrokenGlass = card("Shard of Broken Glass") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0.\n" +
        "Whenever equipped creature attacks, you may mill two cards.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        optional = true
        effect = Patterns.Library.mill(2)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "262"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a4924d7-f787-474b-beca-3b369903485c.jpg?1783937706"
    }
}
