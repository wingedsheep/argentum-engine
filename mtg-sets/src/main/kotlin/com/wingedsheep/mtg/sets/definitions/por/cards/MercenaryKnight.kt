package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Costs

/**
 * Mercenary Knight
 * {2}{B}
 * Creature — Human Mercenary Knight
 * 4/4
 * When Mercenary Knight enters the battlefield, sacrifice it unless you
 * discard a creature card.
 */
val MercenaryKnight = card("Mercenary Knight") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Mercenary Knight"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(
            cost = Costs.pay.Discard(GameObjectFilter.Creature),
            suffer = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Paolo Parente"
        flavorText = "A mercenary's loyalty lasts only as long as the coin."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec9f97a2-b04e-418b-89c7-1c019288f27a.jpg"
    }
}
