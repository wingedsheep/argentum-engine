package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.StaticTarget
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
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, StaticTarget.SourceCreature),
            condition = Conditions.IsYourTurn
        )
    }

    triggeredAbility {
        trigger = Triggers.CreatureDealtDamageByThisDies
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "214"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13f4bafe-0d21-47ba-8f16-0274107d618c.jpg?1562782879"
    }
}
