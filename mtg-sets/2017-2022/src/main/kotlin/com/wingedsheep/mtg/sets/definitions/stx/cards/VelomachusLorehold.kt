package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Velomachus Lorehold
 * {5}{R}{W}
 * Legendary Creature — Elder Dragon
 * 5/5
 *
 * Flying, vigilance, haste
 * Whenever Velomachus Lorehold attacks, look at the top seven cards of your library. You may cast
 * an instant or sorcery spell with mana value less than or equal to Velomachus Lorehold's power
 * from among them without paying its mana cost. Put the rest on the bottom of your library in a
 * random order.
 *
 * Sunbird's Invocation / Kiora, Sovereign of the Deep's pipeline: look (not reveal) at the top
 * seven, choose up to one instant or sorcery whose mana value is at most Velomachus's power (read
 * at resolution, last-known if it has left) and cast it during the trigger's resolution via
 * [Effects.CastFromCollectionWithoutPayingCost]; then, as Plargg, Dean of Chaos does, every looked-at
 * card still in the library goes to the bottom in a random order.
 */
val VelomachusLorehold = card("Velomachus Lorehold") {
    manaCost = "{5}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying, vigilance, haste\n" +
        "Whenever Velomachus Lorehold attacks, look at the top seven cards of your library. You may cast an " +
        "instant or sorcery spell with mana value less than or equal to Velomachus Lorehold's power from " +
        "among them without paying its mana cost. Put the rest on the bottom of your library in a random order."

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        description = "Whenever Velomachus Lorehold attacks, look at the top seven cards of your library. You " +
            "may cast an instant or sorcery spell with mana value less than or equal to Velomachus Lorehold's " +
            "power from among them without paying its mana cost. Put the rest on the bottom of your library " +
            "in a random order."
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(7, Player.You))
            val chosen = chooseUpTo(
                1,
                from = looked,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.InstantOrSorcery
                    .manaValueAtMostDynamic(DynamicAmounts.sourcePower()),
                showAllCards = true,
                prompt = "You may cast an instant or sorcery spell with mana value up to Velomachus " +
                    "Lorehold's power without paying its mana cost.",
                selectedLabel = "Cast for free",
                remainderLabel = "Put on bottom",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
            // "The rest" is every looked-at card not cast — including a chosen card that could
            // not actually be cast — so bottom whatever is still in the library afterwards.
            val notCast = filter(looked, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            toLibraryBottom(notCast, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04ea9067-ad50-4c43-ae4d-3f66890cd898.jpg?1783927292"
        ruling(
            "2021-04-16",
            "When Velomachus Lorehold's triggered ability resolves, it lets you cast a spell immediately, " +
                "ignoring timing restrictions. It does not let you cast that spell later in the turn.",
        )
        ruling(
            "2021-04-16",
            "If you choose not to cast a spell this way, all of the cards will be put on the bottom of the " +
                "library in a random order.",
        )
        ruling(
            "2021-04-16",
            "If you're casting a spell with {X} in its cost without paying its mana cost, you must choose 0 " +
                "for the value of X.",
        )
    }
}
