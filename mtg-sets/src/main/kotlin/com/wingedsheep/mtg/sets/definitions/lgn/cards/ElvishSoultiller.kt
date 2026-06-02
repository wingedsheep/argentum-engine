package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Elvish Soultiller
 * {3}{G}{G}
 * Creature — Elf Mutant
 * 5/4
 * When Elvish Soultiller dies, choose a creature type. Shuffle all creature cards
 * of that type from your graveyard into your library.
 */
val ElvishSoultiller = card("Elvish Soultiller") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Mutant"
    power = 5
    toughness = 4
    oracleText = "When Elvish Soultiller dies, choose a creature type. Shuffle all creature cards of that type from your graveyard into your library."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Composite(
            listOf(
                ChooseCreatureTypeEffect,
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                    storeAs = "graveyardCreatures"
                ),
                SelectFromCollectionEffect(
                    from = "graveyardCreatures",
                    selection = SelectionMode.All,
                    matchChosenCreatureType = true,
                    storeSelected = "chosen"
                ),
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Ron Spears"
        flavorText = "Mutated elves wondered if this was their final form, or if it was just another step."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e2c8de5-bc80-4fad-af09-6d0a639f6e18.jpg?1562926879"
    }
}
