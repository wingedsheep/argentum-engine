package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.SacrificeSelfEffect

/**
 * Thundering Wurm
 * {2}{G}
 * Creature — Wurm
 * 4/4
 * When Thundering Wurm enters the battlefield, sacrifice it unless you discard a land card.
 */
val ThunderingWurm = card("Thundering Wurm") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Wurm"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(
            cost = PayCost.Discard(filter = CardFilter.LandCard),
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
