package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Military Discipline
 * {W}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When this Aura enters, enchanted creature gains first strike until end of turn.
 * Enchanted creature gets +1/+0.
 */
val MilitaryDiscipline = card("Military Discipline") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, enchanted creature gains first strike until end of turn.\n" +
        "Enchanted creature gets +1/+0."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94778e17-87c4-4765-b2f0-40455069f2c4.jpg"
    }
}
