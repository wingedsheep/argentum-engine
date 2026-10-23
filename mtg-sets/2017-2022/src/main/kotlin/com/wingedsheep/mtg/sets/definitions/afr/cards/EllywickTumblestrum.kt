package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ellywick Tumblestrum — {2}{G}{G} Legendary Planeswalker — Ellywick, loyalty 4.
 *
 * The −7 emblem's "+2/+2 for each differently named dungeon you've completed" is an owned
 * `GrantDynamicStats` over `DungeonsCompleted(distinctNames = true)`: it is re-read every
 * projection, so completing a new dungeon after the emblem exists grows the bonus, and completing
 * the same dungeon again does not.
 */
val EllywickTumblestrum = card("Ellywick Tumblestrum") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Ellywick"
    startingLoyalty = 4
    oracleText = "+1: Venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "−2: Look at the top six cards of your library. You may reveal a creature card from among them " +
        "and put it into your hand. If it's legendary, you gain 3 life. Put the rest on the bottom of " +
        "your library in a random order.\n" +
        "−7: You get an emblem with \"Creatures you control have trample and haste and get +2/+2 for " +
        "each differently named dungeon you've completed.\""

    loyaltyAbility(+1) {
        effect = Effects.VentureIntoTheDungeon()
    }

    loyaltyAbility(-2) {
        effect = Effects.Pipeline(
            descriptionOverride = "Look at the top six cards of your library. You may reveal a creature " +
                "card from among them and put it into your hand. If it's legendary, you gain 3 life. " +
                "Put the rest on the bottom of your library in a random order."
        ) {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (kept, rest) = chooseUpToSplit(
                count = 1,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal a creature card and put it into your hand",
                showAllCards = true
            )
            move(kept, CardDestination.ToZone(Zone.HAND), revealed = true)
            run(
                Effects.If(
                    Conditions.CollectionContainsMatch(kept, GameObjectFilter.Any.legendary()),
                    Effects.GainLife(3)
                )
            )
            move(
                rest,
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        }
    }

    loyaltyAbility(-7) {
        val creaturesYouControl = GroupFilter(GameObjectFilter.Creature.youControl())
        val bonus = DynamicAmounts.dungeonsCompleted(distinctNames = true) * 2
        effect = Effects.CreatePermanentEmblem(
            groupFilter = creaturesYouControl,
            grantedKeywords = listOf(Keyword.TRAMPLE.name, Keyword.HASTE.name),
            ownedStaticAbilities = listOf(GrantDynamicStats(creaturesYouControl, bonus, bonus)),
            emblemDescription = "Creatures you control have trample and haste and get +2/+2 for each " +
                "differently named dungeon you've completed."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bab81bc0-0369-469e-9296-6d9681697b21.jpg?1783926464"
    }
}
