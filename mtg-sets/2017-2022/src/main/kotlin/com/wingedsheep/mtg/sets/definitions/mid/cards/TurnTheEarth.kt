package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Turn the Earth
 * {G}
 * Instant
 * Choose up to three target cards in graveyards. The owners of those cards shuffle them into their
 * libraries. You gain 2 life.
 * Flashback {1}{G}
 *
 * The chosen targets are gathered and moved as one collection: each card is routed to its owner's
 * library and every library that received a card is shuffled exactly once (per the rulings). With
 * no targets chosen nothing moves and no library is shuffled — the `ifNotEmpty` guard keeps the
 * collection move from falling back to shuffling the caster's library. If every chosen target is
 * illegal on resolution the spell doesn't resolve, so no life is gained.
 */
val TurnTheEarth = card("Turn the Earth") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose up to three target cards in graveyards. The owners of those cards shuffle them " +
        "into their libraries. You gain 2 life.\n" +
        "Flashback {1}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        targets(TargetFilter.CardInGraveyard, count = 3, optional = true)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            ifNotEmpty(chosen) {
                move(chosen, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled))
            }
        } then Effects.GainLife(2)
    }

    keywordAbility(KeywordAbility.flashback("{1}{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34076a93-9e8f-45d4-a1d7-31f40210a5b6.jpg?1783925569"
        ruling("2021-09-24", "You may choose no targets for Turn the Earth. In that case, no libraries will be shuffled and you'll just gain 2 life.")
        ruling("2021-09-24", "Each affected library is shuffled only once, even if multiple cards are being shuffled into it.")
        ruling("2021-09-24", "If you choose target cards in graveyards and all of those targets are somehow illegal as Turn the Earth tries to resolve (perhaps because your opponent responded with a Turn the Earth of their own), the spell will not resolve and you will gain no life.")
    }
}
