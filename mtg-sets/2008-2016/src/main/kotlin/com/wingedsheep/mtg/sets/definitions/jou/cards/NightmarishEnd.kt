package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nightmarish End
 * {2}{B}
 * Instant
 * Target creature gets -X/-X until end of turn, where X is the number of cards in your hand.
 *
 * Feeding Frenzy's shape with a hand count: X is read once on resolution and locked in.
 */
val NightmarishEnd = card("Nightmarish End") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -X/-X until end of turn, where X is the number of cards in your hand."

    spell {
        val t = target(TargetFilter.Creature)
        val handCount = -DynamicAmounts.cardsInYourHand()
        effect = Effects.ModifyStats(power = handCount, toughness = handCount, target = t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Willian Murai"
        flavorText = "\"To think some believe it peaceful to die in one's sleep.\"\n—Ashiok, Nightmare Weaver"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bab0e0a-7eb9-4422-a1d4-82cc05b4374d.jpg?1783939434"
    }
}
