package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sigarda's Imprisonment
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block.
 * {4}{W}: Exile enchanted creature. Create a Blood token.
 *
 * "Can't attack or block" is the two-static-ability Pacifism idiom ([CantAttack] + [CantBlock]
 * over the attached creature). The activated ability exiles the enchanted creature and makes a
 * Blood token.
 */
val SigardasImprisonment = card("Sigarda's Imprisonment") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "{4}{W}: Exile enchanted creature. Create a Blood token. (It's an artifact with \"{1}, " +
        "{T}, Discard a card, Sacrifice this token: Draw a card.\")"

    auraTarget = Targets.Creature

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.Composite(
            Effects.Exile(EffectTarget.EnchantedCreature),
            Effects.CreateBlood(1),
        )
        description = "{4}{W}: Exile enchanted creature. Create a Blood token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/118995a6-8471-47ac-9404-700ce7fc46f6.jpg?1782703171"
    }
}
