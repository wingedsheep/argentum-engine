package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chill of Foreboding
 * {2}{U}
 * Sorcery
 * Each player mills five cards.
 * Flashback {7}{U}
 *
 * Visits every player in turn order via [Effects.ForEachPlayer], rebinding the controller to the
 * visited player for the mill, as Fascination's mill mode does.
 */
val ChillOfForeboding = card("Chill of Foreboding") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player mills five cards.\n" +
        "Flashback {7}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.ForEachPlayer(Player.Each, Patterns.Library.mill(5))
    }
    keywordAbility(KeywordAbility.flashback("{7}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Terese Nielsen"
        flavorText = "\"Wait . . . did you fellows hear something?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0abd6534-92bb-44e3-88c2-6709f1a4f29c.jpg?1783940844"
    }
}
