package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shining Armor
 * {1}{W}
 * Artifact — Equipment
 * Flash
 * When this Equipment enters, attach it to target Knight you control.
 * Equipped creature gets +0/+2 and has vigilance.
 * Equip {3}
 *
 * Bramble Armor's ETB-attach shape. "Target Knight" is a bare tribal noun, so it's any Knight
 * permanent you control rather than only a creature; attaching to a noncreature Knight leaves
 * the Equipment unattached by state-based action.
 */
val ShiningArmor = card("Shining Armor") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, attach it to target Knight you control.\n" +
        "Equipped creature gets +0/+2 and has vigilance.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.KNIGHT).youControl()))
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(0, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/7791f46e-f772-4d3f-824e-52b4fc721b58.jpg?1783932669"
    }
}
