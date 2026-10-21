package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kiora, Sovereign of the Deep — March of the Machine: The Aftermath #35
 * {3}{G}{U} · Legendary Creature — Merfolk Noble · 4/5
 *
 * Vigilance, ward {3}
 * Whenever you cast a Kraken, Leviathan, Octopus, or Serpent spell from your hand, look at the
 * top X cards of your library, where X is that spell's mana value. You may cast a spell with
 * mana value less than X from among them without paying its mana cost. Put the rest on the
 * bottom of your library in a random order.
 *
 * The trigger is Sunbird's Invocation's shape with a subtype-filtered spell and a strict
 * "less than X" cap (X − 1 as an at-most bound). The cards are only looked at, not revealed.
 */
val KioraSovereignOfTheDeep = card("Kiora, Sovereign of the Deep") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Noble"
    power = 4
    toughness = 5
    oracleText = "Vigilance, ward {3}\n" +
        "Whenever you cast a Kraken, Leviathan, Octopus, or Serpent spell from your hand, look at " +
        "the top X cards of your library, where X is that spell's mana value. You may cast a spell " +
        "with mana value less than X from among them without paying its mana cost. Put the rest on " +
        "the bottom of your library in a random order."

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))

    triggeredAbility {
        trigger = Triggers.you.casts(
            spell = GameObjectFilter.Any.withAnySubtype("Kraken", "Leviathan", "Octopus", "Serpent"),
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        description = "Whenever you cast a Kraken, Leviathan, Octopus, or Serpent spell from your " +
            "hand, look at the top X cards of your library, where X is that spell's mana value. You " +
            "may cast a spell with mana value less than X from among them without paying its mana " +
            "cost. Put the rest on the bottom of your library in a random order."
        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(
                    count = DynamicAmounts.triggeringManaValue(),
                    player = Player.You,
                )
            )
            val (chosen, toBottom) = chooseUpToSplit(
                1,
                from = looked,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland
                    .manaValueAtMostDynamic(DynamicAmounts.triggeringManaValue() - 1),
                showAllCards = true,
                prompt = "You may cast a spell with mana value less than X without paying its mana cost.",
                selectedLabel = "Cast for free",
                remainderLabel = "Put on bottom"
            )
            move(
                toBottom,
                CardDestination.ToZone(
                    Zone.LIBRARY,
                    placement = ZonePlacement.Bottom,
                ),
                order = CardOrder.Random
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "35"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b599f53-614c-4b1f-9899-15d5d1e35879.jpg?1783916513"
        ruling("2023-05-12", "The spell you cast without paying its mana cost is cast during the resolution of the triggered ability. Timing restrictions of that spell based on card type are ignored. It will resolve before the spell that caused the ability to trigger.")
        ruling("2023-05-12", "If you cast a spell \"without paying its mana cost,\" you can't pay any alternative costs. You can, however, pay additional costs. If the spell has any mandatory additional costs, those must be paid to cast it.")
        ruling("2023-05-12", "If the spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
