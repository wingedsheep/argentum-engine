package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Short Sword
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * Equip {1}
 */
val ShortSword = card("Short Sword") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\nEquip {1}"

    staticAbility {
        effect = Effects.ModifyStats(+1, +1)
        filter = Filters.EquippedCreature
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        timing = TimingRule.SorcerySpeed
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "229"
        artist = "John Severin Brassell"
        flavorText = "\"Sometimes the only difference between a martyr and a hero is a sword.\" —Captain Sisay, Memoirs"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb79a623-21c3-4310-bc76-310935511d45.jpg?1592322777"
    }
}
