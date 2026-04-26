package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spiral into Solitude
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 * {1}{W}, Blight 1, Sacrifice this Aura: Exile enchanted creature.
 * (To blight 1, put a -1/-1 counter on a creature you control.)
 */
val SpiralIntoSolitude = card("Spiral into Solitude") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "{1}{W}, Blight 1, Sacrifice this Aura: Exile enchanted creature. " +
        "(To blight 1, put a -1/-1 counter on a creature you control.)"

    auraTarget = Targets.Creature

    staticAbility {
        ability = CantAttack(target = StaticTarget.AttachedCreature)
    }

    staticAbility {
        ability = CantBlock(target = StaticTarget.AttachedCreature)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W}"),
            Costs.Blight(1),
            Costs.SacrificeSelf,
        )
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        description = "{1}{W}, Blight 1, Sacrifice this Aura: Exile enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Drew Baker"
        flavorText = "Paranoia can drive kithkin to isolate themselves. Then their neighbors begin to whisper, and their fears become true."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7a12664-a930-4159-8311-19862488fb05.jpg?1767871731"
    }
}
