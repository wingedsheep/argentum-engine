package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

val MidnightTilling = card("Midnight Tilling") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill four cards, then you may return a permanent card from among them to your hand. (To mill four cards, put the top four cards of your library into your graveyard.)"

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                    storeAs = "milled"
                ),
                MoveCollectionEffect(
                    from = "milled",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                ),
                SelectFromCollectionEffect(
                    from = "milled",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Permanent,
                    storeSelected = "selected",
                    showAllCards = true,
                    prompt = "You may return a permanent card to your hand",
                    selectedLabel = "Return to hand",
                    remainderLabel = "Leave in graveyard"
                ),
                MoveCollectionEffect(
                    from = "selected",
                    destination = CardDestination.ToZone(Zone.HAND)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Slawomir Maniak"
        flavorText = "The kithkin farmer awoke to find the crop ruined and a trail of rootprints leading into the forest."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5112bbf-752c-41e7-9c61-4c81e6a77463.jpg?1767732843"
    }
}
