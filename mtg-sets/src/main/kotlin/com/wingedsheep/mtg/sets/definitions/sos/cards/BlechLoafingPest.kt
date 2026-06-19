package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blech, Loafing Pest — Secrets of Strixhaven #176
 * {1}{B}{G} · Legendary Creature — Pest · 3/4
 *
 * Whenever you gain life, put a +1/+1 counter on each Pest, Bat, Insect, Snake,
 * and Spider you control.
 *
 * The [Triggers.YouGainLife] trigger fires on any life-gaining event you have. Its
 * effect fans out via [Effects.ForEachInGroup] over every creature you control whose
 * subtype is one of the five tribes ([GameObjectFilter.withAnySubtype]) — including
 * Blech itself, who is a Pest. Inside the per-creature iteration the counter targets
 * [EffectTarget.Self] (the current iteration entity), not a context target.
 */
val BlechLoafingPest = card("Blech, Loafing Pest") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Pest"
    power = 3
    toughness = 4
    oracleText = "Whenever you gain life, put a +1/+1 counter on each Pest, Bat, " +
        "Insect, Snake, and Spider you control."

    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Creature
                    .withAnyOfSubtypes(
                        listOf(Subtype("Pest"), Subtype.BAT, Subtype.INSECT, Subtype.SNAKE, Subtype.SPIDER)
                    )
                    .youControl()
            ),
            effect = AddCountersEffect(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.Self
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Ilse Gort"
        flavorText = "Despite Blex's passing, Witherbloom students still continued the " +
            "tradition of a yearly search in his honor. His son, however, took little " +
            "effort to find."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f588fa50-7cc5-41ba-90df-2d252eb5c785.jpg?1775938208"
    }
}
