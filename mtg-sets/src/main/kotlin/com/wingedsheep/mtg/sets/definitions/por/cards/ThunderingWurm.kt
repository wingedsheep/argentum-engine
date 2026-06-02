package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.dsl.Costs

/**
 * Thundering Wurm
 * {2}{G}
 * Creature — Wurm
 * 4/4
 * When Thundering Wurm enters the battlefield, sacrifice it unless you discard a land card.
 */
val ThunderingWurm = card("Thundering Wurm") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(
            cost = Costs.pay.Discard(GameObjectFilter.Land),
            suffer = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Greg Staples"
        flavorText = "Its passage shakes the earth like thunder."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b0ba623-d17f-4f0e-b914-da139a3971df.jpg"
    }
}
