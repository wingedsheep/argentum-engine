package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bladed Battle-Fan
 * {1}{B}
 * Artifact — Equipment
 * Flash
 * When this Equipment enters, attach it to target creature you control. That creature gains
 * indestructible until end of turn.
 * Equipped creature gets +1/+0.
 * Equip {1}
 */
val BladedBattleFan = card("Bladed Battle-Fan") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, attach it to target creature you control. That creature gains " +
        "indestructible until end of turn.\n" +
        "Equipped creature gets +1/+0.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(creature) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
    }

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a28f5de4-2af6-44ff-9bb8-d874f8ae7dd1.jpg?1783917017"
    }
}
