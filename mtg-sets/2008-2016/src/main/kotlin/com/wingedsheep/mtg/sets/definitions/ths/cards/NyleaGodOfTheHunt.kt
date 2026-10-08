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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nylea, God of the Hunt
 * {3}{G}
 * Legendary Enchantment Creature — God
 * 6/6
 *
 * Indestructible
 * As long as your devotion to green is less than five, Nylea isn't a creature.
 * Other creatures you control have trample.
 * {3}{G}: Target creature gets +2/+2 until end of turn.
 *
 * The Theros god clause is a Layer 4 [RemoveCardType] of CREATURE on Nylea herself, gated by
 * `DevotionTo(GREEN) < 5` (CR 700.5) — the same conditional type removal Impending uses. Statics
 * only apply on the battlefield, so Nylea is a creature card everywhere else and a creature spell
 * on the stack (ruling). Devotion counts Nylea's own {G} once she's on the battlefield. Losing the
 * creature type also strips the creature type God (CR 205.1a) and removes an attacking or blocking
 * Nylea from combat (CR 506.4).
 */
val NyleaGodOfTheHunt = card("Nylea, God of the Hunt") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment Creature — God"
    power = 6
    toughness = 6
    oracleText = "Indestructible\n" +
        "As long as your devotion to green is less than five, Nylea isn't a creature. (Each {G} in the mana " +
        "costs of permanents you control counts toward your devotion to green.)\n" +
        "Other creatures you control have trample.\n" +
        "{3}{G}: Target creature gets +2/+2 until end of turn."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.GREEN), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.OtherCreaturesYouControl)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "166"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f185a734-a32a-4244-88e8-dabafbfd064f.jpg?1783939744"
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
