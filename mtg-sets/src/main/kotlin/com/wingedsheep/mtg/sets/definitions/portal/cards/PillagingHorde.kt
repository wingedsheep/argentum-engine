package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.SacrificeSelfEffect

/**
 * Pillaging Horde
 * {2}{R}{R}
 * Creature — Human Barbarian
 * 5/5
 * When Pillaging Horde enters the battlefield, sacrifice it unless you discard a card at random.
 */
val PillagingHorde = card("Pillaging Horde") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Human Barbarian"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = OnEnterBattlefield()
        effect = PayOrSufferEffect(
            cost = PayCost.Discard(random = true),
            suffer = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Tom Wänerstrand"
        flavorText = "They take what they want and burn the rest."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1daad744-e6b2-4bd8-83df-2e97e9e60d16.jpg"
    }
}
