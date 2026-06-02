package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Frenzied Tilling
 * {3}{R}{G}
 * Sorcery
 * Destroy target land. Search your library for a basic land card, put that card
 * onto the battlefield tapped, then shuffle.
 */
val FrenziedTilling = card("Frenzied Tilling") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Search your library for a basic land card, " +
        "put that card onto the battlefield tapped, then shuffle."

    spell {
        target = TargetPermanent(filter = TargetFilter.Land)
        effect = Effects.Composite(
            Effects.Destroy(EffectTarget.ContextTarget(0)),
            LibraryPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "Mike Raabe"
        flavorText = "\"Beneath her scars, Dominaria's beauty yet shines.\"\n—Multani, maro-sorcerer"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15875876-3341-40fb-866f-5587c3638538.jpg?1562899335"
    }
}
