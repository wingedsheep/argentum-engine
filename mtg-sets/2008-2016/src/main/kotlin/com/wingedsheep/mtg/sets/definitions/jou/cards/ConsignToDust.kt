package com.wingedsheep.mtg.sets.definitions.jou.cards

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
 * Consign to Dust — Journey into Nyx #119 (canonical printing)
 * {2}{G} · Instant
 *
 * Strive — This spell costs {2}{G} more to cast for each target beyond the first.
 * Destroy any number of target artifacts and/or enchantments.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {2}{G} per
 * [CostReductionSource.ChosenTargets] beyond the first; the mana value stays 3.
 *
 * "Any number" is an unlimited target count with no floor — zero targets is a legal cast (ruling).
 * Each still-legal target is destroyed on resolution; illegal ones are skipped (CR 608.2b).
 */
val ConsignToDust = card("Consign to Dust") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {2}{G} more to cast for each target beyond the first.\n" +
        "Destroy any number of target artifacts and/or enchantments."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{2}{G}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.ArtifactOrEnchantment, unlimited = true)
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Andreas Rocha"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee28c0ba-7305-4b64-8cb8-3ecd9c284c4a.jpg?1783939415"
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
