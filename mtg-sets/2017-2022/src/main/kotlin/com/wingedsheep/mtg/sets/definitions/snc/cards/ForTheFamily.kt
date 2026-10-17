package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * For the Family
 * {G}
 * Instant
 * Target creature gets +2/+2 until end of turn. If you control four or more creatures, that
 * creature gets +4/+4 until end of turn instead.
 *
 * The creature count is checked on resolution; both branches buff the same target.
 */
val ForTheFamily = card("For the Family") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If you control four or more creatures, that creature gets +4/+4 until end of turn instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.ControlCreaturesAtLeast(4),
            then = Effects.ModifyStats(4, 4, t),
            otherwise = Effects.ModifyStats(2, 2, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Kieran Yanner"
        flavorText = "The Cabaretti cultivate an image of harmless hedonism, happy for the other families to underestimate their formidable firepower."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/052be93c-1970-4d05-a3d1-e87a6e51c423.jpg?1783923103"
    }
}
