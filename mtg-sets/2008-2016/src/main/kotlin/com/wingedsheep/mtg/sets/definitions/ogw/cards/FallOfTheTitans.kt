package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fall of the Titans
 * {X}{X}{R}
 * Instant
 * Surge {X}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Fall of the Titans deals X damage to each of up to two targets.
 *
 * The surge cost carries a single {X}, so a surged cast pays X once where the mana cost pays it
 * twice; X is announced as usual either way and the damage reads it. The spell's mana value still
 * comes from {X}{X}{R} (ruling below).
 */
val FallOfTheTitans = card("Fall of the Titans") {
    manaCost = "{X}{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Surge {X}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Fall of the Titans deals X damage to each of up to two targets."

    keywordAbility(KeywordAbility.surge("{X}{R}"))

    spell {
        target = AnyTarget(count = 2, minCount = 0, optional = true)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(DynamicAmounts.xValue(), EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Chris Rallis"
        flavorText = "As Gideon kept the brood lineages at bay, Chandra incinerated the bound Eldrazi titans."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c21da73-0dad-4b2b-8cc6-61861bed9410.jpg?1783937906"
        ruling("2016-01-22", "Fall of the Titan's mana value is based on its mana cost of {X}{X}{R}, even if you're casting it for its surge cost. For example, if you cast Fall of the Titans for its surge cost and choose 4 for X, its mana value will be 9.")
    }
}
