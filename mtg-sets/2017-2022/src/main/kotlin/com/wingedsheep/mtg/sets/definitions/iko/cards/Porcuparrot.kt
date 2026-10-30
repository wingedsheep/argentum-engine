package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Porcuparrot
 * {3}{R}
 * Creature — Bird Beast
 * 3/4
 * Mutate {2}{R}
 * {T}: This creature deals X damage to any target, where X is the number of times this creature has mutated.
 */
val Porcuparrot = card("Porcuparrot") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Bird Beast"
    power = 3
    toughness = 4
    oracleText = "Mutate {2}{R} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "{T}: This creature deals X damage to any target, where X is the number of times this creature has mutated."

    keywordAbility(KeywordAbility.mutate("{2}{R}"))

    activatedAbility {
        cost = Costs.Tap
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.TimesMutated),
            any,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/856892c8-ba47-46d0-aec2-0416b55b9e88.jpg"
    }
}
