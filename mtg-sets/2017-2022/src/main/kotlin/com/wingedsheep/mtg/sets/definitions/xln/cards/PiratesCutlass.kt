package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pirate's Cutlass
 * {3}
 * Artifact — Equipment
 * When this Equipment enters, attach it to target Pirate you control.
 * Equipped creature gets +2/+1.
 * Equip {2}
 */
val PiratesCutlass = card("Pirate's Cutlass") {
    manaCost = "{3}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target Pirate you control.\n" +
        "Equipped creature gets +2/+1.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val pirate = target(TargetFilter.PermanentYouControl.withSubtype(Subtype.PIRATE))
        effect = Effects.AttachEquipment(pirate)
    }

    staticAbility {
        ability = ModifyStats(+2, +1, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3e7e871-19cf-486d-bacb-1499fe066974.jpg?1782710346"
    }
}
