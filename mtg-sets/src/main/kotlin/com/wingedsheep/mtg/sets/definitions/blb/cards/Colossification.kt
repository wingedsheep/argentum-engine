package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Colossification
 * {5}{G}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature gets +20/+20.
 */
val Colossification = card("Colossification") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, tap enchanted creature.\nEnchanted creature gets +20/+20."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = ModifyStats(20, 20)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "392"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2cad902-ffd2-4bab-b114-b4b6df2ac6b3.jpg?1721428099"
        inBooster = false
    }
}
