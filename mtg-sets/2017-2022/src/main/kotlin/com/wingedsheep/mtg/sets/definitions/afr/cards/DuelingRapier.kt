package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dueling Rapier
 * {R}
 * Artifact — Equipment
 * Flash
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+0.
 * Equip {4}
 */
val DuelingRapier = card("Dueling Rapier") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\nWhen this Equipment enters, attach it to target creature you control.\nEquipped creature gets +2/+0.\nEquip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)"
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }
    staticAbility {
        ability = ModifyStats(2, 0)
    }
    equipAbility("{4}")
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c304118a-c83b-490a-b0e2-721734a996ee.jpg?1783926480"
    }
}
