package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blossombind
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature can't become untapped and can't have counters put on it.
 */
val Blossombind = card("Blossombind") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, tap enchanted creature.\nEnchanted creature can't become untapped and can't have counters put on it."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    staticAbility {
        ability = CantReceiveCounters()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Drew Tucker"
        flavorText = "He wished for isolation, and Shadowmoor replied."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/382b83c0-bbbc-4db8-bc04-cfea79aed1b3.jpg?1767732510"
    }
}
