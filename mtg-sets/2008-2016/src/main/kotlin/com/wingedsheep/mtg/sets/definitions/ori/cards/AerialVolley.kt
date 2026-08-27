package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aerial Volley
 * {G}
 * Instant
 * Aerial Volley deals 3 damage divided as you choose among one, two, or three target creatures
 * with flying.
 */
val AerialVolley = card("Aerial Volley") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Aerial Volley deals 3 damage divided as you choose among one, two, or three target creatures with flying."

    spell {
        target = TargetObject(
            filter = TargetFilter.Creature.withKeyword(Keyword.FLYING),
            count = 3,
            minCount = 1
        )
        effect = DividedDamageEffect(
            totalDamage = 3,
            minTargets = 1,
            maxTargets = 3
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Lake Hurwitz"
        flavorText = "Drakes can swerve to avoid a single arrow, but they can't dodge the whole sky."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5178301-f766-48d3-af07-6bd6f822c725.jpg?1783938325"

        ruling("2015-06-22", "You choose how many targets Aerial Volley has and how the damage is divided as you cast the spell. Each target must receive at least 1 damage.")
        ruling("2015-06-22", "If some (but not all) of the targets become illegal before Aerial Volley tries to resolve, the original division of damage still applies, but no damage is dealt to illegal targets. If all targets become illegal, Aerial Volley won't resolve.")
    }
}
