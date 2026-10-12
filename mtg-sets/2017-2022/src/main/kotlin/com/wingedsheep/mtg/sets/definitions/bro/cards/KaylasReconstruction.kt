package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Kayla's Reconstruction
 * {X}{W}{W}{W}
 * Sorcery
 *
 * Look at the top seven cards of your library. Put up to X artifact and/or creature cards with
 * mana value 3 or less from among them onto the battlefield. Put the rest on the bottom of your
 * library in a random order.
 *
 * Collected Company's shape with an X cap: [Patterns.Library.lookAtTopAndTakeMatching] looks at the
 * top seven privately, the caster takes *up to X* ([SelectionMode.ChooseUpTo] over
 * [DynamicAmounts.xValue], the spell's announced X) of the cards that are artifacts and/or
 * creatures with mana value 3 or less — each card individually, per the ruling — puts them onto
 * the battlefield together, and bottoms the rest in a random order.
 */
val KaylasReconstruction = card("Kayla's Reconstruction") {
    manaCost = "{X}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Look at the top seven cards of your library. Put up to X artifact and/or creature cards with mana value 3 or less from among them onto the battlefield. Put the rest on the bottom of your library in a random order."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(7),
            filter = GameObjectFilter.CreatureOrArtifact.manaValueAtMost(3),
            prompt = "Put up to X artifact and/or creature cards with mana value 3 or less onto the battlefield",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.xValue()),
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Nicholas Elias"
        flavorText = "Anyone can destroy a continent. Few have the vision to rebuild one."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bb84e72-0231-461a-a230-d8afa6e54289.jpg?1783920129"
        ruling("2022-10-14", "Each of the cards needs to have mana value 3 or less. It's not the total mana value of the cards.")
    }
}
