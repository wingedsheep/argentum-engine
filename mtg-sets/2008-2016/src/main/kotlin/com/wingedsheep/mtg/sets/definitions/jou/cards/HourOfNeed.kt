package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hour of Need — Journey into Nyx #40 (canonical printing)
 * {2}{U} · Instant
 *
 * Strive — This spell costs {1}{U} more to cast for each target beyond the first.
 * Exile any number of target creatures. For each creature exiled this way, its controller creates
 * a 4/4 blue Sphinx creature token with flying.
 *
 * Strive is one [CostModification.IncreaseManaPerUnit] of {1}{U} per
 * [CostReductionSource.ChosenTargets] beyond the first; the mana value stays 3.
 *
 * For each still-legal target (CR 608.2b skips illegal ones) the creature is exiled with a tracked
 * move, and the Sphinx is created only if that move actually happened ("exiled this way" — a
 * replacement that sends it elsewhere makes no token). The token goes to the creature's
 * controller as it last existed on the battlefield ([EffectTarget.TargetController] reads the
 * re-scoped per-target context and falls back to last-known information once the card is gone),
 * so a stolen creature's token goes to the player who controlled it, not its owner.
 */
val HourOfNeed = card("Hour of Need") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Strive — This spell costs {1}{U} more to cast for each target beyond the first.\n" +
        "Exile any number of target creatures. For each creature exiled this way, its controller " +
        "creates a 4/4 blue Sphinx creature token with flying."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseManaPerUnit(
                symbols = "{1}{U}",
                countSource = CostReductionSource.ChosenTargets(beyond = 1),
            ),
        )
    }

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val creature = gather(CardSource.Entity(EffectTarget.ContextTarget(0)))
                    moveTracked(creature, CardDestination.ToZone(Zone.EXILE), name = "exiled")
                },
                then = Effects.CreateToken(
                    power = 4,
                    toughness = 4,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Sphinx"),
                    keywords = setOf(Keyword.FLYING),
                    controller = EffectTarget.TargetController,
                    imageUri = "https://cards.scryfall.io/normal/front/b/7/b74f1983-e9ac-47f0-836d-843a53c054cc.jpg?1783939394"
                ),
                successCriterion = SuccessCriterion.CollectionNonEmpty("exiled")
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/737b859d-d81e-4da3-a8bf-9a8f23c75a50.jpg?1783939448"
        ruling("2014-04-26", "You choose how many targets each spell with a strive ability has and what those targets are as you cast it. It's legal to cast such a spell with no targets, although this is rarely a good idea. You can't choose the same target more than once for a single strive spell.")
        ruling("2014-04-26", "The mana cost and mana value of strive spells don't change no matter how many targets they have. Strive abilities affect only what you pay.")
        ruling("2014-04-26", "If all of the spell's targets are illegal when the spell tries to resolve, it won't resolve and none of its effects will happen. If one or more of its targets are legal when it tries to resolve, the spell will resolve and affect only those legal targets. It will have no effect on any illegal targets.")
        ruling("2014-04-26", "If such a spell is copied, and the effect that copies the spell allows a player to choose new targets for the copy, the number of targets can't be changed. The player may change any number of the targets, including all of them or none of them. If, for one of the targets, the player can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2014-04-26", "If a spell or ability allows you to cast a strive spell without paying its mana cost, you must pay the additional costs for any targets beyond the first.")
    }
}
