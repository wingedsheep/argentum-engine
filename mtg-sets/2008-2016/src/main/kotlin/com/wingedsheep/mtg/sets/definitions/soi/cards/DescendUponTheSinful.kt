package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Descend upon the Sinful — Shadows over Innistrad #13
 * {4}{W}{W} · Sorcery
 *
 * Exile all creatures.
 * Delirium — Create a 4/4 white Angel creature token with flying if there are four or more card
 * types among cards in your graveyard.
 *
 * Delirium is checked as the spell resolves, after the sweep, while the spell itself is still on
 * the stack — so it never counts its own sorcery type (per the ruling). Exiled creatures never
 * reach a graveyard, so the sweep itself can't enable delirium.
 */
val DescendUponTheSinful = card("Descend upon the Sinful") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile all creatures.\nDelirium — Create a 4/4 white Angel creature token with flying " +
        "if there are four or more card types among cards in your graveyard."

    spell {
        effect = Effects.Pipeline {
            exile(gather(GameObjectFilter.Creature))
        } then Effects.If(
            condition = Conditions.Delirium(),
            then = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/b/d/bdb975fe-ac30-4249-bb55-7efb64645e4d.jpg?1783937685"
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "13"
        artist = "Tyler Jacobson"
        flavorText = "Above the cries, a phrase rang out: \"You will all be saved.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9ff2cbf-a1dc-4cc5-9a5d-8439899d4e87.jpg?1783937822"
        ruling(
            "2016-04-08",
            "If you have three non-sorcery card types among cards in your graveyard at the time Descend upon " +
                "the Sinful resolves, you won't get an Angel token. Descend upon the Sinful isn't put into your " +
                "graveyard until after it's finished resolving."
        )
        ruling(
            "2016-04-08",
            "If one of those creatures was enchanted, its Aura won't be put into a player's graveyard until " +
                "after Descend upon the Sinful has finished resolving. If the controller of Descend upon the " +
                "Sinful owned the Aura, it won't be in the graveyard in time to be counted for the delirium ability."
        )
    }
}
