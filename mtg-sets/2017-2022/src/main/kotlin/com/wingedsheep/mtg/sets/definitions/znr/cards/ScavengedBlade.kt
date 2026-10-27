package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scavenged Blade — Zendikar Rising #157
 * {1}{R} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+0.
 * Equip {2}{R}
 */
val ScavengedBlade = card("Scavenged Blade") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +2/+0.\n" +
        "Equip {2}{R} ({2}{R}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    equipAbility("{2}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3280f0d0-de0d-4f87-a6ef-c941cb7c9784.jpg"
    }
}
