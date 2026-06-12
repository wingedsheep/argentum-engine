package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Eclipsed Boggart
 * {B/R}{B/R}{B/R}
 * Creature — Goblin Scout
 * 2/3
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal a Goblin, Swamp, or Mountain card from among them and put
 * it into your hand. Put the rest on the bottom of your library in a
 * random order.
 */
val EclipsedBoggart = card("Eclipsed Boggart") {
    manaCost = "{B/R}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Creature — Goblin Scout"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal a Goblin, Swamp, or Mountain card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(4)), name = "looked")
            val (kept, rest) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(listOf(
                        CardPredicate.HasSubtype(Subtype.GOBLIN),
                        CardPredicate.HasSubtype(Subtype.SWAMP),
                        CardPredicate.HasSubtype(Subtype.MOUNTAIN),
                        ))
                    )
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true,
                name = "kept",
                remainderName = "rest"
            )
            move(kept, CardDestination.ToZone(Zone.HAND), revealed = true, revealToSelf = false)
            move(rest, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Tiffany Turrill"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ac1d0b2-92a4-4a10-b2d1-e9bb90265cc3.jpg?1767952361"
    }
}
