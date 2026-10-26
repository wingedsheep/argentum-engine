package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Irenicus's Vile Duplication (CLB #78)
 * {3}{U}
 * Sorcery
 * Create a token that's a copy of target creature you control, except the token has flying and it
 * isn't legendary.
 *
 * Both halves of the "except" clause are copiable values (CR 707.9b), carried on [CopyExceptions]:
 * flying is added whether or not the original was legendary (the 2022 Oracle update), and the
 * legendary supertype is removed so the copy of a legend survives the legend rule.
 */
val IrenicussVileDuplication = card("Irenicus's Vile Duplication") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Create a token that's a copy of target creature you control, except the token has " +
        "flying and it isn't legendary."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(
            creature,
            exceptions = CopyExceptions(
                addedKeywords = setOf(Keyword.FLYING),
                removedSupertypes = setOf(Supertype.LEGENDARY)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Oleksandr Kozachenko"
        flavorText = "\"You will serve me better in this form.\"\n—Jon Irenicus"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40a791df-2483-406d-90b0-a8d402d615d6.jpg?1783922787"

        ruling("2022-06-10", "Irenicus's Vile Duplication has received an update to its Oracle text to clarify that the token still has flying even if the creature it's copying isn't legendary.")
    }
}
