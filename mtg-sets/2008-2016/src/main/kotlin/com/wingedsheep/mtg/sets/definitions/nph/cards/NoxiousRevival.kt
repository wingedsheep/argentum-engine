package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Noxious Revival
 * {G/P}
 * Instant
 *
 * ({G/P} can be paid with either {G} or 2 life.)
 * Put target card from a graveyard on top of its owner's library.
 */
val NoxiousRevival = card("Noxious Revival") {
    manaCost = "{G/P}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "({G/P} can be paid with either {G} or 2 life.)\n" +
        "Put target card from a graveyard on top of its owner's library."

    spell {
        val graveyardCard = target(TargetFilter.CardInGraveyard)
        effect = Effects.PutOnTopOfLibrary(graveyardCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Matt Stewart"
        flavorText = "\"Dead or alive, my creations are stronger than Jin-Gitaxias's septic minions.\"\n" +
            "—Vorinclex, Voice of Hunger"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bdd1243-1d14-496a-9b7a-0c5b34461361.jpg?1783941300"
    }
}
