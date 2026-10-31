package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Archipelagore
 * {5}{U}{U}
 * Creature — Leviathan
 * 7/7
 * Mutate {5}{U}
 * Whenever this creature mutates, tap up to X target creatures, where X is the number of times
 * this creature has mutated. Those creatures don't untap during their controller's next untap step.
 *
 * X caps the trigger's target count, read off the merged permanent as the trigger is put on the
 * stack (the mutation has already happened, so the first mutation allows one target).
 */
val Archipelagore = card("Archipelagore") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leviathan"
    power = 7
    toughness = 7
    oracleText = "Mutate {5}{U} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, tap up to X target creatures, where X is the number of times this creature has mutated. Those creatures don't untap during their controller's next untap step."

    keywordAbility(KeywordAbility.mutate("{5}{U}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        targets(
            TargetFilter.Creature,
            optional = true,
            dynamicMaxCount = DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.TimesMutated),
        )
        effect = Effects.TapEachTarget() then
            Effects.ForEachTarget(
                Effects.GrantKeyword(
                    AbilityFlag.DOESNT_UNTAP,
                    EffectTarget.ContextTarget(0),
                    Duration.UntilAfterAffectedControllersNextUntap()
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f24b1030-64d5-4c94-a04c-1d9520bfddab.jpg?1783931079"
    }
}
