package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shower of Arrows
 * {2}{G}
 * Instant
 *
 * Destroy target artifact, enchantment, or creature with flying. Scry 1.
 */
val ShowerOfArrows = card("Shower of Arrows") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact, enchantment, or creature with flying. Scry 1."

    spell {
        val targetFilter = TargetFilter(
            GameObjectFilter.Artifact or GameObjectFilter.Enchantment or
                GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
        )
        val permanent = target(targetFilter)
        effect = Effects.Destroy(permanent) then Patterns.Library.scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Manuel Castañón"
        flavorText = "\"I wish there were more of your kin among us, Gimli. But even more would I give for a hundred good archers of Mirkwood. We shall need them.\"\n—Legolas"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92cd3884-18d1-4200-b28e-a52349ef37aa.jpg?1686969600"
    }
}
