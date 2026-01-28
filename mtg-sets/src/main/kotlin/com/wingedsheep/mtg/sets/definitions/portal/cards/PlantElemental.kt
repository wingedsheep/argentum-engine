package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.SacrificeSelfEffect

/**
 * Plant Elemental
 * {1}{G}
 * Creature — Plant Elemental
 * 3/4
 * When Plant Elemental enters the battlefield, sacrifice it unless you sacrifice a Forest.
 */
val PlantElemental = card("Plant Elemental") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Plant Elemental"
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(
            cost = PayCost.Sacrifice(filter = CardFilter.HasSubtype("Forest")),
            suffer = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Donato Giancola"
        flavorText = "Rooted in the forest's magic, it draws life from the land itself."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/892594db-1d66-4c45-bd54-608a9972ca77.jpg"
    }
}
