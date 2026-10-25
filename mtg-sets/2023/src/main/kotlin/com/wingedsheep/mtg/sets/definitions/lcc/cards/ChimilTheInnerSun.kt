package com.wingedsheep.mtg.sets.definitions.lcc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.core.Step

/**
 * Chimil, the Inner Sun
 * {6}
 * Legendary Artifact
 * Spells you control can't be countered.
 * At the beginning of your end step, discover 5.
 */
val ChimilTheInnerSun = card("Chimil, the Inner Sun") {
    manaCost = "{6}"
    typeLine = "Legendary Artifact"
    oracleText = "Spells you control can't be countered.\nAt the beginning of your end step, discover 5."

    staticAbility {
        ability = GrantCantBeCountered(filter = GameObjectFilter.Any.youControl())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Discover(5)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "106"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfb49910-30fe-483e-b3b8-6268417f013c.jpg"
    }
}
