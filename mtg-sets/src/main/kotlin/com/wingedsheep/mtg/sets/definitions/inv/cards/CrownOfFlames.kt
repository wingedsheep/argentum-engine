package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crown of Flames
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * {R}: Enchanted creature gets +1/+0 until end of turn.
 * {R}: Return this Aura to its owner's hand.
 */
val CrownOfFlames = card("Crown of Flames") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "{R}: Enchanted creature gets +1/+0 until end of turn.\n" +
        "{R}: Return this Aura to its owner's hand."

    auraTarget = Targets.Creature

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.EnchantedCreature)
        description = "{R}: Enchanted creature gets +1/+0 until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        description = "{R}: Return this Aura to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a46239c-3de7-48ca-8f5c-b51f307fd0e5.jpg?1562913336"
    }
}
