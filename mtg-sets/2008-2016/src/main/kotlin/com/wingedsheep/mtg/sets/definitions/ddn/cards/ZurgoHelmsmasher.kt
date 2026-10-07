package com.wingedsheep.mtg.sets.definitions.ddn.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zurgo Helmsmasher
 * {2}{R}{W}{B}
 * Legendary Creature — Orc Warrior
 * 7/2
 * Haste
 * Zurgo Helmsmasher attacks each combat if able.
 * Zurgo Helmsmasher has indestructible as long as it's your turn.
 * Whenever a creature dealt damage by Zurgo Helmsmasher this turn dies,
 * put a +1/+1 counter on Zurgo Helmsmasher.
 */
val ZurgoHelmsmasher = card("Zurgo Helmsmasher") {
    manaCost = "{2}{R}{W}{B}"
    colorIdentity = "WBR"
    typeLine = "Legendary Creature — Orc Warrior"
    power = 7
    toughness = 2
    oracleText = "Haste\nZurgo Helmsmasher attacks each combat if able.\nZurgo Helmsmasher has indestructible as long as it's your turn.\nWhenever a creature dealt damage by Zurgo Helmsmasher this turn dies, put a +1/+1 counter on Zurgo Helmsmasher."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = MustAttack()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.IsYourTurn
        )
    }

    triggeredAbility {
        trigger = Triggers.self.damagedCreatureDies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31f21aae-e25b-4d14-b558-a848a9372f92.jpg?1783939129"
    }
}
