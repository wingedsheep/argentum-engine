package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Teething Wurmlet
 * {G}
 * Creature — Wurm
 * 1/1
 * This creature has deathtouch as long as you control three or more artifacts.
 * Whenever an artifact you control enters, you gain 1 life. If this is the first time this ability
 * has resolved this turn, put a +1/+1 counter on this creature.
 *
 * The deathtouch is a [ConditionalStaticAbility] over [Conditions.YouControlAtLeast] (cf. Chrome
 * Steed / Dhund Operative). The trigger is the "Victor template" (cf. Omnath, Locus of Creation):
 * every resolution gains 1 life, and bumps the per-turn resolution counter before the counter is
 * gated on [Conditions.SourceAbilityResolvedNTimes] (== 1), so only the first resolution this turn
 * grows the Wurmlet.
 */
val TeethingWurmlet = card("Teething Wurmlet") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 1
    toughness = 1
    oracleText = "This creature has deathtouch as long as you control three or more artifacts.\n" +
        "Whenever an artifact you control enters, you gain 1 life. If this is the first time this " +
        "ability has resolved this turn, put a +1/+1 counter on this creature."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = IncrementAbilityResolutionCountEffect then
            Effects.GainLife(1) then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(1),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            )
        description = "Whenever an artifact you control enters, you gain 1 life. If this is the " +
            "first time this ability has resolved this turn, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2350027a-f3a4-4cba-9dfc-27e33bddb3be.jpg?1783920039"
    }
}
