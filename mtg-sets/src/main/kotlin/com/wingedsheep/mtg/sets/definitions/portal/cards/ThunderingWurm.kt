package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SacrificeUnlessDiscardEffect

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
        effect = SacrificeUnlessDiscardEffect(discardFilter = CardFilter.LandCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Greg Staples"
        flavorText = "Its passage shakes the earth like thunder."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2e15b39-8afc-483c-8c0c-3a8e7e5e60c3.jpg"
    }
}
