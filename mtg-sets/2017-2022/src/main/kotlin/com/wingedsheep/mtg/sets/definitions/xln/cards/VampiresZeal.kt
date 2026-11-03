package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vampire's Zeal
 * {W}
 * Instant
 *
 * Target creature gets +2/+2 until end of turn. If it's a Vampire, it gains first strike until
 * end of turn.
 *
 * The Vampire check happens on resolution, after the pump.
 */
val VampiresZeal = card("Vampire's Zeal") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it's a Vampire, it gains first strike until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature) then Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE), creature),
            then = Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Gabor Szikszai"
        flavorText = "The Feast of Blood sends new life essence flowing through the vampire who partakes, manifesting as even greater strength and speed."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88c507f6-c178-40be-bca7-3bd5adb1c5b1.jpg?1783935788"
    }
}
