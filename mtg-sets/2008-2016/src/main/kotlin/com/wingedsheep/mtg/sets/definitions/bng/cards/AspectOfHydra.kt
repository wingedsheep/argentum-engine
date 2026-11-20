package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aspect of Hydra
 * {G}
 * Instant
 *
 * Target creature gets +X/+X until end of turn, where X is your devotion to green.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing. X is read once as the
 * spell resolves and the bonus is locked in (ruling).
 */
val AspectOfHydra = card("Aspect of Hydra") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+X until end of turn, where X is your devotion to green. " +
        "(Each {G} in the mana costs of permanents you control counts toward your devotion to green.)"

    spell {
        val creature = target(TargetFilter.Creature)
        val devotion = DynamicAmounts.devotionTo(Color.GREEN)
        effect = Effects.ModifyStats(devotion, devotion, creature)
    }

    metadata {
        ruling(
            "2014-02-01",
            "The value of X is calculated as Aspect of Hydra resolves. The bonus won't change later in the " +
                "turn, even if your devotion to green does."
        )
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Mark Winters"
        flavorText = "\"Even alone I outnumber you.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19353629-07be-47e2-a7d5-3a5e5e1120c8.jpg?1783939539"
    }
}
