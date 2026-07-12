package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cradle of Safety
 * {1}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature you control
 * When this Aura enters, enchanted creature gains hexproof until end of turn.
 * Enchanted creature gets +1/+1.
 */
val CradleOfSafety = card("Cradle of Safety") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature you control\n" +
        "When this Aura enters, enchanted creature gains hexproof until end of turn. (It can't be " +
        "the target of spells or abilities your opponents control.)\n" +
        "Enchanted creature gets +1/+1."

    keywords(Keyword.FLASH)
    auraTarget = Targets.CreatureYouControl

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42cbad81-152a-435f-9289-f4b6483a059b.jpg?1782703156"
    }
}
