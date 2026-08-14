package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moment of Glory
 * {W}
 * Sorcery
 *
 * Put a +1/+1 counter on target creature you control. If this spell was cast from a
 * graveyard, also put a +1/+1 counter on each other creature you control.
 * Flashback {4}{W}
 *
 * Modeling notes:
 *  - The bonus clause is "cast from *a* graveyard", not "cast with flashback" — any
 *    graveyard-cast path turns it on, which is exactly [Conditions.WasCastFromGraveyard]
 *    (`WasCastFromZoneCondition(Zone.GRAVEYARD)`), not a flashback-specific test.
 *  - "Each **other** creature you control" is other than the *target*, so the group uses
 *    [GroupFilter.otherThanTarget] — `ForEachExecutor` drops the spell's first chosen
 *    target from the iteration set. Without it the target would get two counters.
 *  - The whole spell still has a single target, so an illegal target on resolution fizzles
 *    both halves (CR 608.2b); nothing here is a separate "each creature" mode.
 */
val MomentOfGlory = card("Moment of Glory") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control. If this spell was cast " +
        "from a graveyard, also put a +1/+1 counter on each other creature you control.\n" +
        "Flashback {4}{W} (You may cast this card from your graveyard for its flashback cost. " +
        "Then exile it.)"

    spell {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature),
            ConditionalEffect(
                condition = Conditions.WasCastFromGraveyard,
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()).otherThanTarget(),
                    Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                ),
            ),
        )
    }

    keywordAbility(KeywordAbility.flashback("{4}{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Jarel Threat"
        flavorText = "\"I am Bard, slayer of the Dragon!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a6a6ff0-b1cd-4b06-bd31-612690094e0e.jpg?1785497012"
    }
}
