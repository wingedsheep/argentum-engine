package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thassa, God of the Sea — Theros #66
 * {2}{U} · Legendary Enchantment Creature — God · 5/5
 *
 * Indestructible
 * As long as your devotion to blue is less than five, Thassa isn't a creature.
 * At the beginning of your upkeep, scry 1.
 * {1}{U}: Target creature you control can't be blocked this turn.
 *
 * The god clause is the Heliod/Nylea shape: a Layer 4 [RemoveCardType] of CREATURE on Thassa
 * herself, gated by `DevotionTo(BLUE) < 5`.
 */
val ThassaGodOfTheSea = card("Thassa, God of the Sea") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to blue is less than five, Thassa isn't a creature. (Each {U} in the mana " +
        "costs of permanents you control counts toward your devotion to blue.)\n" +
        "At the beginning of your upkeep, scry 1.\n" +
        "{1}{U}: Target creature you control can't be blocked this turn."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.BLUE), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Scry(1)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        val t = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
        description = "{1}{U}: Target creature you control can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "66"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6876c7a-8bbe-484e-b733-70229fa336cd.jpg?1783939788"
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "If a God is attacking or blocking and it stops being a creature, it will be removed from combat. It " +
                "won't rejoin combat if it resumes being a creature later during that combat."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
        ruling(
            "2013-09-15",
            "Mana symbols in the text boxes of permanents you control don't count toward your devotion to any color."
        )
        ruling(
            "2013-09-15",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count toward " +
                "your devotion to their color(s)."
        )
    }
}
