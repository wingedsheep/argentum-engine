package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ShoreLurker = card("Shore Lurker") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Merfolk Scout"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, surveil 1. " +
        "(Look at the top card of your library. You may put it into your graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Tiffany Turrill"
        flavorText = "\"The Dark Meanders contain nothing but refuse and the trinkets " +
            "other merrow have left behind. I'm on the hunt for something new.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb353c27-e311-4677-9277-cab6820562ce.jpg?1767732498"
    }
}
