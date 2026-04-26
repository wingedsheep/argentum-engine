package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Temporal Cleansing
 * {3}{U}
 * Sorcery
 *
 * Convoke
 * The owner of target nonland permanent puts it into their library second
 * from the top or on the bottom.
 */
val TemporalCleansing = card("Temporal Cleansing") {
    manaCost = "{3}{U}"
    typeLine = "Sorcery"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "The owner of target nonland permanent puts it into their library second from the top or on the bottom."

    keywords(Keyword.CONVOKE)

    spell {
        val t = target("target nonland permanent", Targets.NonlandPermanent)
        effect = Effects.PutSecondFromTopOrBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Wylie Beckert"
        flavorText = "Careless fae become lost in dreamstuff too sweet."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50ee7315-ec53-43d2-841e-8ec192b850f1.jpg?1767732550"
    }
}
