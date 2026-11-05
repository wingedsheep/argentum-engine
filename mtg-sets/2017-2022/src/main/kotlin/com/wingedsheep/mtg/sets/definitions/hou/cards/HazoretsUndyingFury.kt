package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hazoret's Undying Fury
 * {4}{R}{R}
 * Sorcery
 * Shuffle your library, then exile the top four cards. You may cast any number of spells with
 * mana value 5 or less from among them without paying their mana costs. Lands you control don't
 * untap during your next untap step.
 *
 * The free casts happen during resolution (2017-07-14 ruling); anything not cast — lands included —
 * stays in exile. The untap clause mirrors Bontu's Last Reckoning: not locked at resolution, so it
 * also holds back lands that enter afterwards.
 */
val HazoretsUndyingFury = card("Hazoret's Undying Fury") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Shuffle your library, then exile the top four cards. You may cast any number of spells with mana value 5 or less from among them without paying their mana costs. Lands you control don't untap during your next untap step."

    spell {
        effect = Effects.ShuffleLibrary() then
            Effects.Pipeline {
                val exiled = gather(CardSource.TopOfLibrary(4))
                exile(exiled)
                val castable = filter(exiled, GameObjectFilter.Nonland.manaValueAtMost(5))
                run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(castable))
            } then
            Effects.SkipUntap(EffectTarget.Controller, GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f578576-19ca-4e67-94da-2c0767d9a2bb.jpg?1783936027"
        ruling("2017-07-14", "No lands that you control will untap during your next untap step, even lands that aren't tapped as this spell resolves. This includes lands that enter the battlefield after this spell resolves.")
        ruling("2017-07-14", "Each individual spell you cast this way must have mana value 5 or less. Their total mana value could be greater.")
        ruling("2017-07-14", "No player may take any actions between the time you shuffle your library and the time you exile the top four cards.")
        ruling("2017-07-14", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs, such as emerge costs. You can, however, pay additional costs. If the card has any mandatory additional costs, such as that of Tormenting Voice, you must pay those to cast the card.")
        ruling("2017-07-14", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2017-07-14", "If any abilities trigger as you cast any of those cards, they won't be put on the stack until after you're done casting them. They'll resolve before any of those spells.")
        ruling("2017-07-14", "If you cast any of those cards, you do so as part of the resolution of Hazoret's Undying Fury. You can't wait to cast them later in the turn.")
        ruling("2017-07-14", "If a spell you cast this way targets another spell, it may target a spell you cast earlier during the resolution of Hazoret's Undying Fury. It may target Hazoret's Undying Fury as well, but Hazoret's Undying Fury will be put into your graveyard soon afterwards and the spell's target will become illegal.")
        ruling("2017-07-14", "If more than one spell says that lands you control don't untap during your next untap step, the effects will all wear off during that untap step. You'll untap lands you control during your untap step after that one.")
    }
}
