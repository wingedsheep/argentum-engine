package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Not Forgotten (Shadows over Innistrad #30)
 * {1}{W}
 * Sorcery
 *
 * Put target card from a graveyard on your choice of the top or bottom of its owner's library.
 * Create a 1/1 white Spirit creature token with flying.
 *
 * "Your choice" means the spell's controller picks the position, not the card's owner, so this is a
 * controller-routed `ChooseAction` between the top and bottom moves rather than
 * `PutOnTopOrBottomOfLibrary` (which asks the owner).
 */
val NotForgotten = card("Not Forgotten") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put target card from a graveyard on your choice of the top or bottom of its owner's library. " +
        "Create a 1/1 white Spirit creature token with flying."

    spell {
        val graveyardCard = target(TargetFilter.CardInGraveyard)
        effect = Effects.ChooseAction(
            choices = listOf(
                EffectChoice("Put it on top of its owner's library", Effects.PutOnTopOfLibrary(graveyardCard)),
                EffectChoice("Put it on the bottom of its owner's library", Effects.PutOnBottomOfLibrary(graveyardCard)),
            ),
        ) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Darek Zabrocki"
        flavorText = "Visit the dead, and they may return the favor."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66ecdadf-56f7-4dfb-9c3c-7d240ba19b00.jpg?1783937815"
    }
}
