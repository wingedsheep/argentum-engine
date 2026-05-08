package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Singing Bell Strike
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * When Singing Bell Strike enters the battlefield, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 * Enchanted creature has "{6}: Untap this creature."
 */
val SingingBellStrike = card("Singing Bell Strike") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, tap enchanted creature.\nEnchanted creature doesn't untap during its controller's untap step.\nEnchanted creature has \"{6}: Untap this creature.\""

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Mana("{6}"),
                effect = Effects.Untap(EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/670f24e1-dec9-42e6-b762-c447366ed16c.jpg?1562787818"
    }
}
