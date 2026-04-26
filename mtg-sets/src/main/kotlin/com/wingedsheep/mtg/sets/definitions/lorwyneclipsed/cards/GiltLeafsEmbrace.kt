package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gilt-Leaf's Embrace
 * {2}{G}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When this Aura enters, enchanted creature gains trample and indestructible until end of turn.
 * Enchanted creature gets +2/+0.
 */
val GiltLeafsEmbrace = card("Gilt-Leaf's Embrace") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, enchanted creature gains trample and indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it. If its toughness is 0 or less, it still dies.)\n" +
        "Enchanted creature gets +2/+0."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.EnchantedCreature)
            .then(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.EnchantedCreature))
    }

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/739e5ab5-d562-407a-906e-c5c5173ad325.jpg?1767957211"
    }
}
