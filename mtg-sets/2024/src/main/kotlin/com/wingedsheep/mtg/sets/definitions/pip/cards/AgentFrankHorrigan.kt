package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Agent Frank Horrigan — indestructible is gated on [Conditions.SourceAttackedThisTurn], which reads
 * the turn's declared-attacker record: it switches on at declaration and holds for the rest of the
 * turn, and a Frank put onto the battlefield attacking was never declared, so it doesn't get it.
 * "Enters or attacks" is the two triggers; each proliferates twice, choosing afresh each time.
 */
val AgentFrankHorrigan = card("Agent Frank Horrigan") {
    manaCost = "{5}{B}{G}"
    typeLine = "Legendary Creature — Mutant Warrior"
    power = 8
    toughness = 6
    oracleText = "Trample\n" +
        "Agent Frank Horrigan has indestructible as long as it attacked this turn.\n" +
        "Whenever Agent Frank Horrigan enters or attacks, proliferate twice. " +
        "(To proliferate, choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, Filters.Self),
            condition = Conditions.SourceAttackedThisTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Proliferate() then Effects.Proliferate()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Proliferate() then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91193a0b-9087-404f-b397-815e29d0fe12.jpg?1783912393"
        ruling("2024-03-08", "The ability that causes Agent Frank Horrigan to have indestructible starts to apply as soon as it's declared as an attacker. It applies for the remainder of the turn. It doesn't matter what happens to the player or permanent it attacked after that point.")
        ruling("2024-03-08", "If Agent Frank Horrigan enters the battlefield attacking, it doesn't have indestructible since it was never declared as an attacker.")
        ruling("2024-03-08", "If you proliferate twice, you don't have to choose the same set of players and/or permanents to get additional counters each time.")
    }
}
