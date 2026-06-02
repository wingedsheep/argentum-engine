package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

val EtaliPrimalStorm = card("Etali, Primal Storm") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Elder Dinosaur"
    oracleText = "Whenever Etali, Primal Storm attacks, exile the top card of each player's library, then you may cast any number of spells from among those cards without paying their mana costs."
    power = 6
    toughness = 6

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.Fixed(1),
                        player = Player.Each
                    ),
                    storeAs = "exiledCards"
                ),
                MoveCollectionEffect(
                    from = "exiledCards",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                FilterCollectionEffect(
                    from = "exiledCards",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.Nonland),
                    storeMatching = "castable"
                ),
                // Cast any number of them for free, during this trigger's resolution (the
                // controller can't wait until later in the turn — see the 2021-03-19 ruling).
                CastAnyNumberFromCollectionWithoutPayingCostEffect("castable")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Raymond Swanland"
        flavorText = "The storm rages and the earth breaks."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd1b0e5-b197-4e8a-a34f-e0b16a928fb0.jpg?1721429153"
        ruling("2021-03-19", "If you cast any of the exiled cards, you do so as part of the resolution of the triggered ability. You can't wait to cast them later in the turn.")
        ruling("2021-03-19", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can, however, pay additional costs.")
        ruling("2021-03-19", "Any cards not cast, including land cards, remain in exile. They can't be cast on later turns, even if Etali attacks again.")
        ruling("2021-03-19", "Because all attacking creatures are chosen at once, a creature cast this way can't attack during the same combat as Etali, even if it has haste.")
    }
}
