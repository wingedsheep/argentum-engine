package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Erebos, God of the Dead — Theros #85
 * {3}{B} · Legendary Enchantment Creature — God · 5/7
 *
 * Indestructible
 * As long as your devotion to black is less than five, Erebos isn't a creature.
 * Your opponents can't gain life.
 * {1}{B}, Pay 2 life: Draw a card.
 *
 * The god clause is the Thassa/Heliod shape: a Layer 4 [RemoveCardType] of CREATURE on Erebos
 * herself, gated by `DevotionTo(BLACK) < 5`. "Your opponents can't gain life" is the
 * [PreventLifeGain] replacement on each opponent (as Archfiend of Despair); it works whether or
 * not Erebos is currently a creature.
 */
val ErebosGodOfTheDead = card("Erebos, God of the Dead") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 7
    oracleText = "Indestructible\n" +
        "As long as your devotion to black is less than five, Erebos isn't a creature. (Each {B} in the mana " +
        "costs of permanents you control counts toward your devotion to black.)\n" +
        "Your opponents can't gain life.\n" +
        "{1}{B}, Pay 2 life: Draw a card."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.BLACK), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent)))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.PayLife(2))
        effect = Effects.DrawCards(1)
        description = "{1}{B}, Pay 2 life: Draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "85"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0787e1f-0b75-44ab-a8fd-90358906a787.jpg"
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
