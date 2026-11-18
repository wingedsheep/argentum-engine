package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Silence the Believers — Journey into Nyx #82 (canonical printing)
 * {2}{B}{B} · Instant
 *
 * Strive — This spell costs {2}{B} more to cast for each target beyond the first.
 * Exile any number of target creatures and all Auras attached to them.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {2}{B} per
 * [CostReductionSource.ChosenTargets] beyond the first; the mana value stays 4.
 *
 * For each still-legal target (CR 608.2b skips illegal ones) the Auras attached to it are gathered
 * *before* anything moves, then the creature and those Auras are exiled in the same resolution —
 * so the Auras never sit unattached long enough for state-based actions to put them into a
 * graveyard (the SoulNova / Mark of Eviction shape). A bestowed Aura is gathered as an Aura and
 * is exiled with its host, as the ruling requires. Every card goes to its owner's exile.
 */
val SilenceTheBelievers = card("Silence the Believers") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {2}{B} more to cast for each target beyond the first.\n" +
        "Exile any number of target creatures and all Auras attached to them."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{2}{B}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.Pipeline {
                val auras = gather(
                    CardSource.AttachedTo(
                        host = EffectTarget.ContextTarget(0),
                        filter = GameObjectFilter.Permanent.withSubtype(Subtype.AURA),
                    )
                )
                run(Effects.Exile(EffectTarget.ContextTarget(0)))
                exile(auras)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6dc2bd1-a980-4eab-8bb3-1ce28c3f9ef4.jpg?1783939430"
        ruling("2014-04-26", "The creatures and Auras are exiled at the same time. Specifically, if an Aura is a permanent with bestow, it will be exiled. It won't stop being an Aura and remain on the battlefield.")
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
