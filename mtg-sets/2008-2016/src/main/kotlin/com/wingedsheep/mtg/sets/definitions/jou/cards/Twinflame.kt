package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Twinflame — Journey into Nyx #115 (canonical printing)
 * {1}{R} · Sorcery
 *
 * Strive — This spell costs {2}{R} more to cast for each target beyond the first.
 * Choose any number of target creatures you control. For each of them, create a token that's a
 * copy of that creature, except it has haste. Exile those tokens at the beginning of the next end
 * step.
 *
 * Strive is the Ajani's Presence shape: one [CostModification.IncreaseManaPerUnit] of {2}{R} per
 * [CostReductionSource.ChosenTargets] beyond the first, and an unlimited target list with no floor.
 *
 * Each still-legal target gets one Heat Shimmer-style copy. The exile is `exileAtStep`, a delayed
 * trigger the spell sets up for the tokens it created — not an ability on the token — so a
 * creature that later copies one of the tokens gets haste but isn't exiled (ruling).
 */
val Twinflame = card("Twinflame") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Strive — This spell costs {2}{R} more to cast for each target beyond the first.\n" +
        "Choose any number of target creatures you control. For each of them, create a token " +
        "that's a copy of that creature, except it has haste. Exile those tokens at the beginning " +
        "of the next end step."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{2}{R}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.CreatureYouControl, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.CreateTokenCopyOfTarget(
                EffectTarget.ContextTarget(0),
                addedKeywords = setOf(Keyword.HASTE),
                exileAtStep = Step.END,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/207128b3-2de3-495a-bf29-eec50c3bd752.jpg?1783939418"
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
        ruling("2014-04-26", "If another creature becomes or enters the battlefield as a copy of the token, that creature will have haste, but you won't exile it. However, if Twinflame creates multiple tokens copying a single creature due to a replacement effect (like the one Doubling Season creates), you'll exile each of them.")
        ruling("2014-04-26", "The tokens copy exactly what was printed on the original creature and nothing else (unless that permanent is copying something else or is a token; see below). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
    }
}
