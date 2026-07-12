package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pointed Discussion
 * {2}{B}
 * Sorcery
 *
 * You draw two cards, lose 2 life, then create a Blood token.
 */
val PointedDiscussion = card("Pointed Discussion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw two cards, lose 2 life, then create a Blood token. (It's an artifact with " +
        "\"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Effects.Composite(
            Effects.DrawCards(2),
            Effects.LoseLife(2, EffectTarget.Controller),
            Effects.CreateBlood(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Jarel Threat"
        flavorText = "Small talk is a dying art."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/076deb63-f7e2-498f-a66a-8a190370a3b3.jpg?1782703101"
    }
}
