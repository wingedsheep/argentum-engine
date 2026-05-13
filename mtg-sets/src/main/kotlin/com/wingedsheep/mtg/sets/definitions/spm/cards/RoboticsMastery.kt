package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Robotics Mastery
 * {4}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * When Robotics Mastery enters the battlefield, create two 1/1 colorless Robot artifact creature tokens with flying.
 */
val RoboticsMastery = card("Robotics Mastery") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\nEnchanted creature gets +2/+2.\nWhen Robotics Mastery enters the battlefield, create two 1/1 colorless Robot artifact creature tokens with flying."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(2),
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Robot"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Domenico Cava"
        flavorText = "Mendel Stromm didn't just seek revenge. He engineered it."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0e92939-6d86-44b4-8a43-1963e97a2bb3.jpg?1757377005"
    }
}
