package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Strike It Rich — Modern Horizons 2 #143
 * {R} · Sorcery
 *
 * Create a Treasure token. (It's an artifact with "{T}, Sacrifice this token: Add one mana of any color.")
 * Flashback {2}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)
 *
 * Treasure is a predefined token, so [Effects.CreateTreasure] carries the token's own tap-sacrifice
 * mana ability — the reminder text in [oracleText] is printed flavour, not a second thing to wire.
 *
 * Flashback is a real [KeywordAbility], not a bare `Keyword`: the engine reads the alternative cost
 * off the keyword ability when it offers the graveyard cast, and exiles the card as it resolves
 * (CR 702.34a). One net card for two casts is why the front half is a single red mana.
 */
val StrikeItRich = card("Strike It Rich") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Flashback {2}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateTreasure()
    }

    keywordAbility(KeywordAbility.flashback("{2}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Volkan Baǵa"
        flavorText = "A few coins soon grow into an obsession."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c7c2814-a617-4123-acdf-1b01b2768210.jpg?1783926837"
    }
}
