package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Keldon Flamesage — X is locked once, at resolution, from the creature's power (its
 * last-known power if it has left, per the ruling), so an enlist boost that resolved first counts.
 * Choosing up to one eligible card is the "may exile"; the rest go to the bottom in a random
 * order, and casting the exiled card is a separate "may".
 */
val KeldonFlamesage = card("Keldon Flamesage") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 3
    oracleText = "Enlist\nWhenever this creature attacks, look at the top X cards of your library, " +
        "where X is this creature's power. You may exile an instant or sorcery card with mana value " +
        "X or less from among them. Put the rest on the bottom of your library in a random order. " +
        "You may cast the exiled card without paying its mana cost."

    staticAbility { ability = Enlist }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val x = storeNumber(DynamicAmounts.sourcePower())
            val looked = gather(CardSource.TopOfLibrary(count = x.amount, player = Player.You))
            val (chosen, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.InstantOrSorcery.manaValueAtMostDynamic(x.amount),
                showAllCards = true,
                prompt = "You may exile an instant or sorcery card with mana value X or less.",
                selectedLabel = "Exile",
                remainderLabel = "Put on bottom",
            )
            exile(chosen)
            toLibraryBottom(rest, order = CardOrder.Random)
            ifNotEmpty(chosen) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(chosen)))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/710d2a3c-63a7-4e33-9ae3-21fb6b3d8326.jpg?1783921315"
        ruling("2022-09-09", "If Keldon Flamesage leaves the battlefield in response to its triggered ability, use its power as it last existed on the battlefield to determine the value of X.")
        ruling("2022-09-09", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as a kicker cost. If the card has any mandatory additional costs, you must pay those to cast the spell.")
        ruling("2022-09-09", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
