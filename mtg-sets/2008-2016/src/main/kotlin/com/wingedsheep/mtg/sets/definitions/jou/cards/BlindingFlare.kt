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
 * Blinding Flare — Journey into Nyx #91 (canonical printing)
 * {R} · Sorcery
 *
 * Strive — This spell costs {R} more to cast for each target beyond the first.
 * Any number of target creatures can't block this turn.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {R} per
 * [CostReductionSource.ChosenTargets] beyond the first; the mana value stays 1.
 *
 * "Any number" is an unlimited target count with no floor, and any creature is legal — including
 * your own (ruling). Each still-legal target gets its own can't-block-this-turn effect.
 */
val BlindingFlare = card("Blinding Flare") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Strive — This spell costs {R} more to cast for each target beyond the first.\n" +
        "Any number of target creatures can't block this turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{R}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Evan Shipard"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e07f43b6-e07f-4d0c-90d2-e5b0d23ba0f9.jpg?1783939427"
        ruling("2014-04-26", "Blinding Flare can target any creature, including those you control.")
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If such a spell is copied, and the effect that copies the spell allows a player to choose new targets for the copy, the number of targets can't be changed. The player may change any number of the targets, including all of them or none of them. If, for one of the targets, the player can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
