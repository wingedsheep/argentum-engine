package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Meltstrider's Gear
 * {G}
 * Artifact — Equipment
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+1 and has reach.
 * Equip {5} ({5}: Attach to target creature you control. Equip only as a sorcery.)
 */
val MeltstridersGear = card("Meltstrider's Gear") {
    manaCost = "{G}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\nEquipped creature gets +2/+1 and has reach.\nEquip {5} ({5}: Attach to target creature you control. Equip only as a sorcery.)"

    // When this Equipment enters, attach it to target creature you control
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
    }

    // Static ability: Equipped creature gets +2/+1 and has reach
    staticAbility {
        effect = Effects.Composite(
            listOf(
                Effects.ModifyStats(+2, +1),
                Effects.GrantKeyword(Keyword.REACH)
            )
        )
        filter = Filters.EquippedCreature
    }

    // Equip {5}
    equipAbility("{5}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Camille Alquier"
        flavorText = "Meltstriders are the bleeding edge of Evendo's frontier thaw."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d75629cb-91e2-46fa-9c80-6feb29e1ceb8.jpg?1752947361"
    }
}
