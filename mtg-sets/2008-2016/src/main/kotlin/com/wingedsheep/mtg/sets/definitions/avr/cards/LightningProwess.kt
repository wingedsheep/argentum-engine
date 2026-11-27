package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lightning Prowess — Avacyn Restored #145 (canonical printing)
 * {2}{R} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has haste and "{T}: This creature deals 1 damage to any target."
 *
 * Prophetic Ravings' haste grant plus Viridian Longbow's granted `{T}` pinger: inside the granted
 * ability [EffectTarget.Self] is the enchanted creature, so it is the damage source.
 */
val LightningProwess = card("Lightning Prowess") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has haste and \"{T}: This creature deals 1 damage to any target.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(
                    amount = 1,
                    target = anyTarget,
                    damageSource = EffectTarget.Self
                )
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "David Rapoza"
        flavorText = "\"Undead flesh is dry and papery. A single spark and 'poof.' No more ghoul.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5578e3e2-2460-4dfb-9016-527463f2d918.jpg?1783940680"
    }
}
