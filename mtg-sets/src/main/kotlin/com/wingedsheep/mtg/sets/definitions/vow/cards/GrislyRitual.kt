package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grisly Ritual
 * {5}{B}
 * Sorcery
 *
 * Destroy target creature or planeswalker. Create two Blood tokens.
 */
val GrislyRitual = card("Grisly Ritual") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature or planeswalker. Create two Blood tokens. (They're " +
        "artifacts with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")"

    spell {
        val t = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        effect = Effects.Composite(
            Effects.Destroy(t),
            Effects.CreateBlood(2),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"You can always tell who's new from the screaming.\"\n—Tinua, Skirsdag cultist"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53cdf2ab-3acd-49bd-8273-84c1cfc92883.jpg?1782703107"
    }
}
