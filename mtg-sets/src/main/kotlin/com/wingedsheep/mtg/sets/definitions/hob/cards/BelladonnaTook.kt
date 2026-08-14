package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Belladonna Took
 * {1}{W}
 * Legendary Creature — Halfling Citizen
 * 2/2
 *
 * Whenever a token you control enters, you gain 1 life if this is the first time this ability has
 * resolved this turn. If it's the second time, draw a card. If it's the third time, put a +1/+1
 * counter on each creature you control.
 *
 * A per-ability resolution counter, not a per-turn trigger cap: the ability keeps triggering all
 * turn, and the payoff is selected by *which* resolution this is. [IncrementAbilityResolutionCountEffect]
 * must run before the three [ConditionalEffect]s read the count, or the first branch would test
 * against 0 and nothing would ever fire (same ordering trap as Elrond, Lord of Rivendell and
 * Harvestrite Host). `SourceAbilityResolvedNTimes` compares for **exact** equality, so the branches
 * are mutually exclusive and the fourth and later resolutions in a turn deliberately do nothing.
 *
 * The trigger is on tokens of any kind, not just creature tokens — a Treasure or a Food entering
 * advances the count just as a Bird does. Belladonna herself is never a token, so she can't trip
 * her own ability by entering.
 */
val BelladonnaTook = card("Belladonna Took") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Halfling Citizen"
    oracleText = "Whenever a token you control enters, you gain 1 life if this is the first time " +
        "this ability has resolved this turn. If it's the second time, draw a card. If it's the " +
        "third time, put a +1/+1 counter on each creature you control."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Any.youControl().token(),
            binding = TriggerBinding.ANY,
        )
        effect = IncrementAbilityResolutionCountEffect
            .then(
                ConditionalEffect(
                    condition = Conditions.SourceAbilityResolvedNTimes(1),
                    effect = Effects.GainLife(1),
                )
            )
            .then(
                ConditionalEffect(
                    condition = Conditions.SourceAbilityResolvedNTimes(2),
                    effect = Effects.DrawCards(1),
                )
            )
            .then(
                ConditionalEffect(
                    condition = Conditions.SourceAbilityResolvedNTimes(3),
                    effect = Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Creature.youControl()),
                        AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                    ),
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Xabi Gaztelua"
        flavorText = "Once in a while, members of the Took clan would go and have adventures."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88f0c189-c9ed-4ea3-ae62-3d8ac6c7fecf.jpg?1784894804"
    }
}
