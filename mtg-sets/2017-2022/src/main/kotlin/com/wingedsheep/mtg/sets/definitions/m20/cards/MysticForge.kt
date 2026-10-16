package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Mystic Forge
 * {4}
 * Artifact
 * You may look at the top card of your library any time.
 * You may cast artifact spells and colorless spells from the top of your library.
 * {T}, Pay 1 life: Exile the top card of your library.
 */
val MysticForge = card("Mystic Forge") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may cast artifact spells and colorless spells from the top of your library.\n" +
        "{T}, Pay 1 life: Exile the top card of your library."

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    // Lands are never cast, so an artifact land on top stays unplayable (2019-07-12 ruling).
    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1))
            exile(exiled)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/924a24e7-91b8-4ceb-a136-7a765d98c994.jpg?1783932941"
        ruling(
            "2019-07-12",
            "Because you never \"cast\" a land card, Mystic Forge doesn't allow you to play an artifact land " +
                "from the top of your library."
        )
        ruling(
            "2019-07-12",
            "You still pay all costs for a spell you cast from your library, including additional costs. " +
                "You may also pay alternative costs."
        )
    }
}
