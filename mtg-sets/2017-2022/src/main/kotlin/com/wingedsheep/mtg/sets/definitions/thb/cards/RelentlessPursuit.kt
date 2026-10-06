package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Relentless Pursuit
 * {2}{G}
 * Sorcery
 * Reveal the top four cards of your library. You may put a creature card and/or a land card from
 * among them into your hand. Put the rest into your graveyard.
 *
 * "A creature card and/or a land card" is two independent up-to-one picks — a creature first, then a
 * land from what's left (the Gift of the Gargantuan shape), so a creature land can fill either slot
 * but never both.
 */
val RelentlessPursuit = card("Relentless Pursuit") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top four cards of your library. You may put a creature card and/or a land card from among them into your hand. Put the rest into your graveyard."

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(4), revealed = true)
            val (creature, afterCreature) = chooseUpToSplit(
                1, from = revealed,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave",
                showAllCards = true
            )
            val (land, rest) = chooseUpToSplit(
                1, from = afterCreature,
                filter = GameObjectFilter.Land,
                prompt = "You may put a land card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put into graveyard",
                showAllCards = true
            )
            toHand(creature)
            toHand(land)
            toGraveyard(rest)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Magali Villeneuve"
        flavorText = "Calix was patient, steadily following Elspeth until their destined confrontation."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/752b9560-b1d6-441c-8bc8-bf6988112d25.jpg?1783931531"
        ruling("2020-01-24", "While resolving Relentless Pursuit, you could put no cards, a creature card, a land card, or a creature card and a land card into your hand.")
    }
}
