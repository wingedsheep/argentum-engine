package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rosethorn Halberd
 * {G}
 * Artifact — Equipment
 * When this Equipment enters, attach it to target non-Human creature you control.
 * Equipped creature gets +2/+1.
 * Equip {5}
 *
 * Bramble Armor's ETB-attach shape with a `notSubtype(Human)` target restriction.
 */
val RosethornHalberd = card("Rosethorn Halberd") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target non-Human creature you control.\n" +
        "Equipped creature gets +2/+1.\n" +
        "Equip {5} ({5}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.notSubtype(Subtype.HUMAN).youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    equipAbility("{5}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Kari Christensen"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4a66e33-af5c-42b5-bef6-0ff0197ecc14.jpg?1783932604"
    }
}
