package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cranial Archive
 * {2}
 * Artifact
 * {2}, Exile Cranial Archive: Target player shuffles their graveyard into their
 * library. Draw a card.
 */
val CranialArchive = card("Cranial Archive") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{2}, Exile this artifact: Target player shuffles their graveyard into their library. Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ExileSelf)
        target("player", Targets.Player)
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.TargetPlayer),
                    storeAs = "graveyardCards"
                ),
                MoveCollectionEffect(
                    from = "graveyardCards",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.TargetPlayer, ZonePlacement.Shuffled)
                ),
                DrawCardsEffect(1)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Volkan Bağa"
        flavorText = "\"The greatest idea the zombie ever had in its head wasn't even its own.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/1284b15f-2a70-48d6-89d3-e787af3f07eb.jpg?1562782804"
    }
}
