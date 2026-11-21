package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Krasis Incubation — Dragon's Maze #79 (canonical printing)
 * {2}{G}{U} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block, and its activated abilities can't be activated.
 * {1}{G}{U}, Return this Aura to its owner's hand: Put two +1/+1 counters on enchanted creature.
 *
 * Arrest's three-static lock, plus a Maze's End-style self-bounce cost. The Aura is already in
 * hand when the ability resolves, so "enchanted creature" is read as last-known information
 * (CR 608.2h): the host it was attached to when the cost was paid.
 */
val KrasisIncubation = card("Krasis Incubation") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block, and its activated abilities can't be activated.\n" +
        "{1}{G}{U}, Return this Aura to its owner's hand: Put two +1/+1 counters on enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}{U}"), Costs.ReturnSelfToHand)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.EnchantedCreature)
        description = "{1}{G}{U}, Return this Aura to its owner's hand: Put two +1/+1 counters on enchanted creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Marco Nelor"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8da986da-e8ee-4b53-8bbd-9285d0f7f3cb.jpg?1783940027"
        ruling(
            "2016-06-08",
            "Activated abilities contain a colon. They're generally written " +
                "\"[Cost]: [Effect].\" Some keywords are activated abilities and will have " +
                "colons in their reminder text."
        )
        ruling(
            "2013-04-15",
            "Returning Krasis Incubation to its owner's hand is part of the cost to activate its " +
                "last ability. Once that ability is announced, players can't respond to it until " +
                "after you've paid its activation cost and returned Krasis Incubation to hand."
        )
    }
}
