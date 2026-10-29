package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Heliod, God of the Sun — Theros #17
 * {3}{W} · Legendary Enchantment Creature — God · 5/6
 *
 * Indestructible
 * As long as your devotion to white is less than five, Heliod isn't a creature.
 * Other creatures you control have vigilance.
 * {2}{W}{W}: Create a 2/1 white Cleric enchantment creature token.
 *
 * The Theros god clause is the Nylea, God of the Hunt shape: a Layer 4 [RemoveCardType] of
 * CREATURE on Heliod himself, gated by `DevotionTo(WHITE) < 5`.
 */
val HeliodGodOfTheSun = card("Heliod, God of the Sun") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 6
    oracleText = "Indestructible\n" +
        "As long as your devotion to white is less than five, Heliod isn't a creature. (Each {W} in the mana " +
        "costs of permanents you control counts toward your devotion to white.)\n" +
        "Other creatures you control have vigilance.\n" +
        "{2}{W}{W}: Create a 2/1 white Cleric enchantment creature token."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.WHITE), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.OtherCreaturesYouControl)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}{W}")
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cleric"),
            enchantmentToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/7/e/7e4c9173-e530-4237-a03b-9aeca60ab0e4.jpg?1783939699"
        )
        description = "{2}{W}{W}: Create a 2/1 white Cleric enchantment creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e90d01c9-e76e-42ff-b0fa-8b6786242aae.jpg?1783939814"
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
