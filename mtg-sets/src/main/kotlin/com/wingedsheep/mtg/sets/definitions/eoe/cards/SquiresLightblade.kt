package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Squire's Lightblade
 * {W}
 * Artifact — Equipment
 * Flash
 * When this Equipment enters, attach it to target creature you control. That creature gains first strike until end of turn.
 * Equipped creature gets +1/+0.
 * Equip {3}
 */
val SquiresLightblade = card("Squire's Lightblade") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\nWhen this Equipment enters, attach it to target creature you control. That creature gains first strike until end of turn.\nEquipped creature gets +1/+0.\nEquip {3}"

    keywords(Keyword.FLASH)

    // ETB: attach to target creature you control, grant first strike until end of turn
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
            .then(Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature))
    }

    // Equipped creature gets +1/+0
    staticAbility {
        ability = ModifyStats(+1, +0, Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a0accba-85d4-4aa4-a70c-80fcce48c261.jpg?1752946695"
    }
}
