package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpell

val PreciseRedaction = card("Precise Redaction") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target white or black spell."

    spell {
        target = TargetSpell(filter = TargetFilter.SpellOnStack.withAnyColor(Color.WHITE, Color.BLACK))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Mathias Kollros"
        flavorText = "Silverquill believed the word was the mightiest weapon. Hexhaven got the last one."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9244efad-35ab-45c0-b173-4bc68276cb67.jpg?1789470790"
        inBooster = false
    }
}
