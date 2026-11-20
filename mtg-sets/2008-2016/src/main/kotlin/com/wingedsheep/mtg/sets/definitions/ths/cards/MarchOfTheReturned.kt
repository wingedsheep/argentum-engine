package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * March of the Returned
 * {3}{B}
 * Sorcery
 * Return up to two target creature cards from your graveyard to your hand.
 */
val MarchOfTheReturned = card("March of the Returned") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creature cards from your graveyard to your hand."

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Mark Zug"
        flavorText = "The Returned have no memory of the Underworld or of their former lives. The golden masks they wear are the last gifts of the selves they left behind."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/983f7521-16e3-4057-a482-7d86392daa57.jpg?1783939775"
    }
}
