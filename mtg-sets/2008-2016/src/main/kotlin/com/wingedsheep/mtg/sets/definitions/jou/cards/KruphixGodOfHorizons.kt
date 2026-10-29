package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConvertEmptyingMana
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kruphix, God of Horizons
 * {3}{G}{U}
 * Legendary Enchantment Creature — God
 * 4/7
 *
 * Indestructible
 * As long as your devotion to green and blue is less than seven, Kruphix isn't a creature.
 * You have no maximum hand size.
 * If you would lose unspent mana, that mana becomes colorless instead.
 *
 * The god clause is Iroas's two-colour devotion [RemoveCardType]. The last ability is Ozai's
 * [ConvertEmptyingMana] in its colorless form (a null colour): the mana is kept, colorless, and a
 * restricted unit keeps its restriction (the 2014 ruling).
 */
val KruphixGodOfHorizons = card("Kruphix, God of Horizons") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Enchantment Creature — God"
    power = 4
    toughness = 7
    oracleText = "Indestructible\n" +
        "As long as your devotion to green and blue is less than seven, Kruphix isn't a creature.\n" +
        "You have no maximum hand size.\n" +
        "If you would lose unspent mana, that mana becomes colorless instead."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.GREEN, Color.BLUE),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility { ability = NoMaximumHandSize }

    staticAbility { ability = ConvertEmptyingMana(null) }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "152"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27427233-da58-45af-ade8-e0727929efaa.jpg?1783939404"
        ruling(
            "2014-04-26",
            "If unspent mana has any restrictions or riders associated with it (for example, if it was produced by " +
                "Cavern of Souls), those restrictions or riders will remain associated with that mana when it becomes colorless."
        )
        ruling(
            "2014-04-26",
            "As long as Kruphix is on the battlefield, you'll keep unspent mana as steps and phases end (although it " +
                "will become colorless). This means you can add mana and spend it during a future step, phase, or turn. " +
                "Once Kruphix leaves the battlefield, you have until the end of the current step or phase to use the " +
                "mana before it disappears."
        )
        ruling(
            "2020-01-24",
            "Your devotion to two colors is the number of mana symbols among mana costs of permanents you control " +
                "that are the first color, the second, or both. If an effect counts your devotion to two colors, a " +
                "hybrid symbol that is both of those colors is counted just once."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The abilities of Gods function as long as they're on the battlefield, regardless of whether they're creatures."
        )
    }
}
