package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Paladin's Shield
 * {1}{W}
 * Artifact — Equipment
 * Flash
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +0/+2.
 * Equip {3}
 */
val PaladinsShield = card("Paladin's Shield") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\nWhen this Equipment enters, attach it to target creature you control.\nEquipped creature gets +0/+2.\nEquip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }
    staticAbility {
        ability = ModifyStats(0, 2)
    }
    equipAbility("{3}")
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e13b103-202f-41d7-a855-a6807e5fb4b7.jpg?1783926526"
    }
}
