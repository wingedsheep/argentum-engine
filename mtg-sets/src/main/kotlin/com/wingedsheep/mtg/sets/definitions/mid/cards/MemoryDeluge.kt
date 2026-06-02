package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

val MemoryDeluge = card("Memory Deluge") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top X cards of your library, where X is the amount of mana spent to cast this spell. Put two of them into your hand and the rest on the bottom of your library in a random order.\nFlashback {5}{U}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = LibraryPatterns.lookAtTopAndKeep(
            count = DynamicAmount.TotalManaSpent,
            keepCount = DynamicAmount.Fixed(2),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }

    keywordAbility(KeywordAbility.flashback("{5}{U}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc00fd1b-3dd9-492a-9ed4-0b6743074730.jpg?1634349038"
    }
}
