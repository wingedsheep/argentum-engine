package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tendrils of Agony
 * {2}{B}{B}
 * Sorcery
 * Target player loses 2 life and you gain 2 life.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val TendrilsOfAgony = card("Tendrils of Agony") {
    manaCost = "{2}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Target player loses 2 life and you gain 2 life.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        val t = target("target player", Targets.Player)
        effect = Effects.Composite(
            Effects.LoseLife(2, t),
            Effects.GainLife(2)
        )
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0559352e-95c1-403b-bd8f-d0679717cfa2.jpg?1562524962"
    }
}
