package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Fires of Victory — the kicked draw happens first, so the damage counts the hand after it
 * (read at resolution, like Master the Way).
 */
val FiresOfVictory = card("Fires of Victory") {
    manaCost = "{1}{R}"
    colorIdentity = "RU"
    typeLine = "Instant"
    oracleText = "Kicker {2}{U} (You may pay an additional {2}{U} as you cast this spell.)\nIf this spell was kicked, draw a card. Fires of Victory deals damage to target creature or planeswalker equal to the number of cards in your hand."

    keywordAbility(KeywordAbility.kicker("{2}{U}"))

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(Conditions.WasKicked, Effects.DrawCards(1)) then
            Effects.DealDamage(DynamicAmounts.cardsInYourHand(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Sidharth Chaturvedi"
        flavorText = "The drums and horns were expected, but the goblin dance was a pleasant surprise."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c58096e5-3cd4-4edd-b71a-3bdb47ab2536.jpg?1783921319"
    }
}
