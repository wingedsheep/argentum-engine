package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mirran Banesplitter — March of the Machine #154.
 * {R} · Artifact — Equipment. Same shape as Malamet Scythe (LCI): flash, attach on entering, equip.
 */
val MirranBanesplitter = card("Mirran Banesplitter") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\nWhen this Equipment enters, attach it to target creature you control.\nEquipped creature gets +2/+0.\nEquip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee4d00b8-1373-47b2-9be5-2199e0b12540.jpg?1783916986"
    }
}
