package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Epic Experiment
 * {X}{U}{R}
 * Sorcery
 * Exile the top X cards of your library. You may cast instant and sorcery spells with mana value X
 * or less from among them without paying their mana costs. Then put all cards exiled this way that
 * weren't cast into your graveyard.
 *
 * The free casts happen during resolution, one at a time, timing restrictions ignored. Afterwards
 * every exiled card still in exile — declined picks, uncastable ones, lands, creatures, spells with
 * mana value above X — goes to the graveyard; the cast ones have already left exile for the stack.
 */
val EpicExperiment = card("Epic Experiment") {
    manaCost = "{X}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Exile the top X cards of your library. You may cast instant and sorcery spells with mana value X or less from among them without paying their mana costs. Then put all cards exiled this way that weren't cast into your graveyard."

    spell {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.xValue()))
            exile(exiled)
            val castable = filter(
                exiled,
                GameObjectFilter.InstantOrSorcery.manaValueAtMostDynamic(DynamicAmounts.xValue()),
            )
            run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(castable))
            val uncast = filter(exiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            toGraveyard(uncast)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42f0b68a-de4b-4c0c-98ac-a812017f88a7.jpg?1783940341"
        ruling("2021-03-19", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, you must pay those to cast the card.")
        ruling("2021-03-19", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2021-03-19", "You cast the cards one at a time, choosing modes, targets, and so on. The last card you cast will be the first one to resolve.")
        ruling("2021-03-19", "When casting an instant or sorcery card this way, ignore timing restrictions based on the card's type. Other timing restrictions, such as \"Cast [this card] only during combat,\" must be followed.")
        ruling("2021-03-19", "If you can't cast a card, perhaps because there are no legal targets available, or if you choose not to cast it, it will be put into your graveyard.")
    }
}
