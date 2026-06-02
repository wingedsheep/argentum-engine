package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Galadhrim Bow
 * {2}{G}
 * Artifact — Equipment
 *
 * Flash
 * When this Equipment enters, attach it to target creature you control. Untap that creature.
 * Equipped creature gets +1/+2 and has reach.
 * Equip {2}
 */
val GaladhrimBow = card("Galadhrim Bow") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\nWhen this Equipment enters, attach it to target creature you control. Untap that creature.\nEquipped creature gets +1/+2 and has reach.\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    // ETB: attach to target creature you control, untap that creature
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
            .then(Effects.Untap(creature))
    }

    // Equipped creature gets +1/+2
    staticAbility {
        ability = ModifyStats(+1, +2, Filters.EquippedCreature)
    }

    // Equipped creature has reach
    staticAbility {
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Daniel Correia"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9df34f21-798f-440d-8b09-ec5b5c0b8c12.jpg?1686969376"
    }
}
