package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CounterRemovalAmount
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Polukranos, Unchained
 * {2}{B}{G}
 * Legendary Creature — Zombie Hydra
 * 0/0
 * Polukranos enters with six +1/+1 counters on it. It escapes with twelve +1/+1 counters on it
 * instead.
 * If damage would be dealt to Polukranos while it has a +1/+1 counter on it, prevent that damage
 * and remove that many +1/+1 counters from it.
 * {1}{B}{G}: Polukranos fights another target creature.
 * Escape—{4}{B}{G}, Exile six other cards from your graveyard.
 *
 * "Six, or twelve instead if it escaped" is two complementary self-only [EntersWithCounters]
 * replacements, one gated on [Conditions.Escaped] and one on its negation, so exactly one applies
 * on any entry (a reanimated or flickered Polukranos didn't escape and gets six).
 *
 * The damage line is Magma Pummeler's [PreventDamageByRemovingCounter] with `requiresCounter =
 * true` ("while it has a +1/+1 counter on it") and `removalAmount = EqualToDamage` ("that many").
 * Damage beyond the counter count is still prevented in full and every counter goes (ruling);
 * damage that can't be prevented is dealt and the counters are still removed (ruling) — the
 * engine's prevent-by-removing path removes the counters and only marks the damage prevented when
 * it can be.
 *
 * The fight is Territorial Allosaurus's [Effects.Fight] over "another target creature".
 */
val PolukranosUnchained = card("Polukranos, Unchained") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Zombie Hydra"
    power = 0
    toughness = 0
    oracleText = "Polukranos enters with six +1/+1 counters on it. It escapes with twelve +1/+1 counters " +
        "on it instead.\n" +
        "If damage would be dealt to Polukranos while it has a +1/+1 counter on it, prevent that damage " +
        "and remove that many +1/+1 counters from it.\n" +
        "{1}{B}{G}: Polukranos fights another target creature.\n" +
        "Escape—{4}{B}{G}, Exile six other cards from your graveyard."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 6,
        selfOnly = true,
        condition = Conditions.Not(Conditions.Escaped)
    ))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 12,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    replacementEffect(
        PreventDamageByRemovingCounter(
            removalAmount = CounterRemovalAmount.EqualToDamage,
            requiresCounter = true
        )
    )

    activatedAbility {
        cost = Costs.Mana("{1}{B}{G}")
        val other = target(TargetFilter.OtherCreature)
        effect = Effects.Fight(EffectTarget.Self, other)
        description = "Polukranos fights another target creature."
    }

    keywordAbility(KeywordAbility.escape("{4}{B}{G}", Costs.additional.ExileOtherCards(6)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "224"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87858278-632c-4d82-b3a3-adfc6f2f4fc6.jpg?1783931519"
        ruling(
            "2020-01-24",
            "If an amount of damage would be dealt to Polukranos greater than the number of +1/+1 counters on " +
                "it, all of that damage is prevented, and all of those counters are removed. In most cases, this " +
                "will result in Polukranos having 0 toughness, so it will be put into its owner's graveyard."
        )
        ruling(
            "2020-01-24",
            "If damage that can't be prevented is dealt to Polukranos, the damage is dealt and you'll " +
                "simultaneously remove that many +1/+1 counters from it, all before the game checks for lethal damage."
        )
        ruling(
            "2020-01-24",
            "If the target creature is an illegal target when Polukranos's activated ability tries to resolve, " +
                "the ability doesn't resolve. If Polukranos is no longer on the battlefield, the target creature " +
                "won't deal or be dealt damage."
        )
    }
}
