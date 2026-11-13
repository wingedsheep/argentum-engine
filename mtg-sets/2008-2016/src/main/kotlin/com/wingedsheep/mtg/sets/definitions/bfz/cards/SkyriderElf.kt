package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.convergeEntersWithCounters
import com.wingedsheep.sdk.model.Rarity

/**
 * Skyrider Elf {X}{G}{U} — Battle for Zendikar #220
 * Creature — Elf Warrior Ally 0/0
 *
 * "Flying
 *  Converge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."
 *
 * Converge is an ability word: modelled via [convergeEntersWithCounters], i.e.
 * `EntersWithDynamicCounters(DynamicAmount.DistinctColorsManaSpent)`. Mana spent on {X} is mana spent
 * to cast it, so paying X with white, black or red raises the count up to five.
 */
val SkyriderElf = card("Skyrider Elf") {
    manaCost = "{X}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Warrior Ally"
    power = 0
    toughness = 0
    oracleText = "Flying\nConverge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."

    keywords(Keyword.FLYING)

    convergeEntersWithCounters()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Dan Murayama Scott"
        flavorText = "\"It may be unconventional, but I'll take any steady mount in a storm.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/7/477eb787-aec7-43fd-9813-184111d9dcc5.jpg?1783938178"
        ruling("2015-08-25", "You can choose any value for X. The value chosen for X doesn't directly affect the number of +1/+1 counters Skyrider Elf enters the battlefield with, but it does let you pay more mana and thus spend more colors of mana to cast it. For example, you can choose 0 for the value of X and pay {G}{U} to cast Skyrider Elf for two +1/+1 counters. You can also choose 3 for the value of X, making its mana cost {3}{G}{U}, and pay {W}{U}{B}{R}{G} for five +1/+1 counters.")
        ruling("2015-08-25", "The maximum number of colors of mana you can spend to cast a spell is five. Colorless is not a color. Note that the cost of a spell with converge may limit how many colors of mana you can spend.")
        ruling("2015-08-25", "If you cast a spell with converge without spending any mana to cast it (perhaps because an effect allowed you to cast it without paying its mana cost), then the number of colors spent to cast it will be zero.")
    }
}
