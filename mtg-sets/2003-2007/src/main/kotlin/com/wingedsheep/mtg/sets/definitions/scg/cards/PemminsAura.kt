package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pemmin's Aura
 * {1}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * {U}: Untap enchanted creature.
 * {U}: Enchanted creature gains flying until end of turn.
 * {U}: Enchanted creature gains shroud until end of turn.
 * {1}: Enchanted creature gets +1/-1 or -1/+1 until end of turn.
 */
val PemminsAura = card("Pemmin's Aura") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n{U}: Untap enchanted creature.\n{U}: Enchanted creature gains flying until end of turn.\n{U}: Enchanted creature gains shroud until end of turn.\n{1}: Enchanted creature gets +1/-1 or -1/+1 until end of turn."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.Untap(EffectTarget.EnchantedCreature)
        description = "{U}: Untap enchanted creature."
    }

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.EnchantedCreature)
        description = "{U}: Enchanted creature gains flying until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(Keyword.SHROUD, EffectTarget.EnchantedCreature)
        description = "{U}: Enchanted creature gains shroud until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Enchanted creature gets +1/-1 until end of turn",
                effect = Effects.ModifyStats(1, -1, EffectTarget.EnchantedCreature),
            ),
            EffectChoice(
                label = "Enchanted creature gets -1/+1 until end of turn",
                effect = Effects.ModifyStats(-1, 1, EffectTarget.EnchantedCreature),
            )
        ))
        description = "{1}: Enchanted creature gets +1/-1 or -1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fb3e38b-086e-4fbc-b7b1-8564c18276d7.jpg?1562532667"
    }
}
