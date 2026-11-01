package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unbreakable Formation
 * {2}{W}
 * Instant
 * Creatures you control gain indestructible until end of turn.
 * Addendum — If you cast this spell during your main phase, put a +1/+1 counter on each of those
 * creatures and they gain vigilance until end of turn.
 *
 * "Those creatures" are the creatures you control at resolution; nothing between the two group
 * walks can change that set (no state-based actions mid-resolution), so the addendum re-walks the
 * same group. Addendum is checked at resolution, as on [ArrestersZeal].
 */
val UnbreakableFormation = card("Unbreakable Formation") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control gain indestructible until end of turn.\n" +
        "Addendum — If you cast this spell during your main phase, put a +1/+1 counter on each of those " +
        "creatures and they gain vigilance until end of turn."

    spell {
        val yourCreatures = GroupFilter(GameObjectFilter.Creature.youControl())

        effect = Effects.ForEachInGroup(
            yourCreatures,
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity),
        ) then Effects.If(
            condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
            then = Effects.ForEachInGroup(
                yourCreatures,
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity) then
                    Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46cc8574-7b8c-492c-8a36-75cb0192f853.jpg?1783933714"
    }
}
