package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Body Dropper
 * {B}{R}
 * Creature — Devil Warrior
 * 2 / 2
 * Whenever you sacrifice another creature, put a +1/+1 counter on this creature.
 * {B}{R}, Sacrifice another creature: This creature gains menace until end of turn.
 */
val BodyDropper = card("Body Dropper") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Devil Warrior"
    oracleText = "Whenever you sacrifice another creature, put a +1/+1 counter on this creature.\n" +
        "{B}{R}, Sacrifice another creature: This creature gains menace until end of turn. " +
        "(It can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.sacrificesAnother(GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}{R}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Jakub Kasper"
        flavorText = "\"Kill count? I lost track years ago.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fcb6d47-dccb-4b69-aed4-7a6215857606.jpg"
    }
}
