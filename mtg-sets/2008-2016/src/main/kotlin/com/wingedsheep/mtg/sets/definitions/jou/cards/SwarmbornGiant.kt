package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swarmborn Giant
 * {2}{G}{G}
 * Creature — Giant
 * 6/6
 * When you're dealt combat damage, sacrifice this creature.
 * {4}{G}{G}: Monstrosity 2. (If this creature isn't monstrous, put two +1/+1 counters on it and it becomes monstrous.)
 * As long as this creature is monstrous, it has reach.
 *
 * "You're dealt combat damage" is the once-per-combat-damage-batch player trigger, by any
 * source ([GameObjectFilter.Any]), not just the creatures this Giant fails to block.
 */
val SwarmbornGiant = card("Swarmborn Giant") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Giant"
    power = 6
    toughness = 6
    oracleText = "When you're dealt combat damage, sacrifice this creature.\n" +
        "{4}{G}{G}: Monstrosity 2. (If this creature isn't monstrous, put two +1/+1 counters on it and it becomes monstrous.)\n" +
        "As long as this creature is monstrous, it has reach."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).dealCombatDamageToYou()
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you're dealt combat damage, sacrifice this creature."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{G}{G}")
        effect = Effects.Monstrosity(2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84a3ddf9-b682-4691-bb1d-abd1d3486f59.jpg?1783939407"
        ruling("2014-04-26", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2014-04-26", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2014-04-26", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
