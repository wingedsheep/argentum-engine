package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quick-Draw Dagger
 * {3}
 * Artifact — Equipment
 * Flash
 * When this Equipment enters, attach it to target creature you control. That creature gains first strike until end of turn.
 * Equipped creature gets +1/+1.
 * Equip {1}
 */
val QuickDrawDagger = card("Quick-Draw Dagger") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, attach it to target creature you control. That creature gains first strike until end of turn.\n" +
        "Equipped creature gets +1/+1.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Ovidio Cartagena"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d64110f4-c4b6-4966-a23a-8da7d1983499.jpg"
    }
}
