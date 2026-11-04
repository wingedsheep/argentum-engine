package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val DjerusRenunciation = card("Djeru's Renunciation") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures.\nCycling {W} ({W}, Discard this card: Draw a card.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.Tap(EffectTarget.ContextTarget(0)))
    }

    keywordAbility(KeywordAbility.cycling("{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Kieran Yanner"
        flavorText = "\"I was promised an afterlife, not this. Samut was right. The God-Pharaoh lied.\"\n—Djeru, former Tah-crop initiate"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03f005de-7971-4f22-ad1f-5d0667f68113.jpg?1783936062"
    }
}
