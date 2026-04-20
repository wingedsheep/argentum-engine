package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Bark of Doran
 * {1}{W}
 * Artifact — Equipment
 * Equipped creature gets +0/+1.
 * As long as equipped creature's toughness is greater than its power, it assigns combat damage
 * equal to its toughness rather than its power.
 * Equip {1}
 */
val BarkOfDoran = card("Bark of Doran") {
    manaCost = "{1}{W}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +0/+1.\n" +
            "As long as equipped creature's toughness is greater than its power, it assigns combat " +
            "damage equal to its toughness rather than its power.\n" +
            "Equip {1}"

    staticAbility {
        effect = Effects.ModifyStats(0, +1)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        ability = AssignDamageEqualToToughness(
            target = StaticTarget.AttachedCreature,
            onlyWhenToughnessGreaterThanPower = true,
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Jorge Jacinto"
        flavorText = "A shield that makes the world tremble."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98210276-1b85-4db5-8ab4-ecb08f5d2ee2.jpg?1767862363"
    }
}
