package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Precognition Field
 * {3}{U}
 * Enchantment
 * You may look at the top card of your library any time.
 * You may cast instant and sorcery spells from the top of your library.
 * {3}: Exile the top card of your library.
 */
val PrecognitionField = card("Precognition Field") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "You may look at the top card of your library any time.\nYou may cast instant and sorcery spells from the top of your library.\n{3}: Exile the top card of your library."

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(filter = GameObjectFilter.InstantOrSorcery)
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "exiled"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf2a92ee-2b20-4299-84b2-b4963f4a42a1.jpg?1562743197"
    }
}
