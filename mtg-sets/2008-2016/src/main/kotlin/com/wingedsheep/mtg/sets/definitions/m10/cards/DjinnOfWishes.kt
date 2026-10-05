package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Djinn of Wishes
 * {3}{U}{U}
 * Creature — Djinn
 * 4/4
 * Flying
 * This creature enters with three wish counters on it.
 * {2}{U}{U}, Remove a wish counter from this creature: Reveal the top card of your library. You may
 *   play that card without paying its mana cost. If you don't, exile it.
 *
 * The play happens while the ability resolves (ruling) through
 * [Effects.PlayFromCollectionWithoutPayingCost]: a spell is cast for free, a land is played from the
 * library and uses up a land play — only on your turn with one left (CR 305.2b, 305.3). Whatever
 * wasn't played, by choice or because it couldn't be, is still in the library and is exiled.
 */
val DjinnOfWishes = card("Djinn of Wishes") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "This creature enters with three wish counters on it.\n" +
        "{2}{U}{U}, Remove a wish counter from this creature: Reveal the top card of your library. " +
        "You may play that card without paying its mana cost. If you don't, exile it."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.WISH,
            count = 3,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}{U}"),
            Costs.RemoveCounterFromSelf(CounterType.WISH, 1)
        )
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1))
            reveal(revealed)
            run(
                Effects.May(
                    Effects.PlayFromCollectionWithoutPayingCost(revealed),
                    descriptionOverride = "Play the revealed card without paying its mana cost"
                )
            )
            exile(filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY)))
        }
        description = "{2}{U}{U}, Remove a wish counter from this creature: Reveal the top card of " +
            "your library. You may play that card without paying its mana cost. If you don't, exile it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e3b0949-17e1-4f12-8999-d4638d32dd3e.jpg?1783942393"
        ruling(
            "2018-07-13",
            "If you wish to play the exiled card, you must play it while the last ability of Djinn of " +
                "Wishes is resolving. You can't play it later in the turn. A spell cast this way may be " +
                "cast at a time you normally wouldn't be able to cast a spell of that type, but other " +
                "restrictions (such as \"Cast this spell only during combat\") are enforced."
        )
        ruling(
            "2018-07-13",
            "If the revealed card is a land, you can play it only if it's your turn and you haven't " +
                "yet played a land this turn."
        )
        ruling(
            "2018-07-13",
            "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any " +
                "alternative costs. You can, however, pay additional costs, such as kicker costs. If the " +
                "card has any mandatory additional costs, such as that of Tormenting Voice, those must be " +
                "paid to cast the card."
        )
        ruling(
            "2018-07-13",
            "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it " +
                "without paying its mana cost."
        )
    }
}
