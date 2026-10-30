package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TotalCap

/**
 * Rampaging Yao Guai — Fallout #82.
 *
 * Both X clauses read [DynamicAmounts.castX] (The Goose Mother idiom): the X announced when the
 * creature was cast, carried on the permanent itself, so the counters and the trigger's cap agree
 * — and a Yao Guai that enters without being cast (a copy, a reanimation) has X = 0 for both
 * (CR 107.3m reads X from the cast; otherwise it is 0).
 *
 * "Any number of target artifacts and/or enchantments with total mana value X or less" is an
 * `unlimited` [TargetObject] with the aggregate [TargetObject.totalAtMost] cap. The cap is
 * baked to a concrete integer when the trigger's targets are chosen (CR 603.3d) and the summed mana
 * value of the chosen permanents is enforced by `DecisionValidators`. A permanent with {X} in its
 * cost counts X as 0, and a plain token has mana value 0 (rulings) — both are what the card's
 * printed mana value already reports.
 */
val RampagingYaoGuai = card("Rampaging Yao Guai") {
    manaCost = "{X}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear Mutant"
    power = 2
    toughness = 2
    oracleText = "Vigilance, trample\n" +
        "This creature enters with X +1/+1 counters on it.\n" +
        "When this creature enters, destroy any number of target artifacts and/or enchantments " +
        "with total mana value X or less."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.castX()))

    triggeredAbility {
        trigger = Triggers.self.enters()
        target = TargetObject(
            filter = TargetFilter.ArtifactOrEnchantment,
            unlimited = true,
            totalAtMost = TotalCap.manaValue(DynamicAmounts.castX()),
        )
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63619236-a27a-4fdd-add6-7a295901f906.jpg?1783912397"
        ruling("2024-03-08", "If a permanent has {X} in its mana cost, X is 0 when determining its mana value.")
        ruling("2024-03-08", "A token that's not a copy of something else normally won't have a mana cost. Anything without a mana cost normally has a mana value of 0.")
    }
}
