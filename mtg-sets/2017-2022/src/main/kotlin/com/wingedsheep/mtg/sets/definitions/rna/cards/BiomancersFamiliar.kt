package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Biomancer's Familiar — Ravnica Allegiance #158
 * {G}{U} · Creature — Mutant · 2 / 2
 *
 * The cost reduction is Training Grounds' [ReduceActivatedAbilityCost] (creatures you control on the
 * battlefield, `manaFloor = 1`). The tap ability drops an [Effects.AdaptAsThoughNoCounters] shield on
 * the target: that creature's next `Effects.Adapt` this turn ignores its +1/+1 counters (CR 701.46a)
 * and uses the shield up.
 */
val BiomancersFamiliar = card("Biomancer's Familiar") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Mutant"
    power = 2
    toughness = 2
    oracleText = "Activated abilities of creatures you control cost {2} less to activate. This effect " +
        "can't reduce the mana in that cost to less than one mana.\n" +
        "{T}: The next time target creature adapts this turn, it adapts as though it had no +1/+1 counters on it."

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            amount = DynamicAmounts.fixed(2),
            manaFloor = 1
        )
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.AdaptAsThoughNoCounters(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d38c9891-36d1-4565-9c4a-1cd9dbf8c048.jpg?1783933657"
        ruling("2019-01-25", "Activated abilities contain a colon. They’re generally written “[Cost]: [Effect].” Some keyword abilities are activated abilities and will have colons in their reminder text. Triggered abilities (starting with “when,” “whenever,” or “at”) are unaffected by the cost reduction ability of Biomancer’s Familiar.")
        ruling("2019-01-25", "Biomancer’s Familiar affects only abilities of creatures you control on the battlefield. The costs of activated abilities of creature cards that work in other zones (such as cycling) won’t be reduced.")
        ruling("2019-01-25", "An ability that has only one generic mana and some amount of other mana symbols in its activation cost will have its cost reduced by {1}.")
        ruling("2019-01-25", "The last ability of Biomancer’s Familiar doesn’t add or remove any counters. It just lets the creature adapt despite already having +1/+1 counters on it.")
    }
}
