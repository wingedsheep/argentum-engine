package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invoke the Ancients — Kamigawa: Neon Dynasty #193 (canonical printing)
 * {1}{G}{G}{G}{G} · Sorcery
 *
 * Create two 4/5 green Spirit creature tokens. For each of them, put your choice of a reach
 * counter, a vigilance counter, or a trample counter on it.
 *
 * The choice is made per token by looping over every token the creation actually produced
 * (`CollectionSlot.CreatedTokens`), so a token doubler (Parallel Lives) yields one choice per
 * resulting token, as the rulings require.
 */
val InvokeTheAncients = card("Invoke the Ancients") {
    manaCost = "{1}{G}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create two 4/5 green Spirit creature tokens. For each of them, put your choice of a " +
        "reach counter, a vigilance counter, or a trample counter on it."

    spell {
        effect = Effects.CreateToken(
            count = 2,
            power = 4,
            toughness = 5,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spirit"),
            imageUri = "https://cards.scryfall.io/normal/front/0/f/0f48aaab-dd6e-4bcc-a8fb-d31dd4a098ba.jpg?1783923712",
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        label = "Reach counter",
                        effect = Effects.AddCounters(CounterType.REACH, 1, EffectTarget.IterationEntity),
                    ),
                    EffectChoice(
                        label = "Vigilance counter",
                        effect = Effects.AddCounters(CounterType.VIGILANCE, 1, EffectTarget.IterationEntity),
                    ),
                    EffectChoice(
                        label = "Trample counter",
                        effect = Effects.AddCounters(CounterType.TRAMPLE, 1, EffectTarget.IterationEntity),
                    ),
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Filip Burburan"
        flavorText = "After large sections were clear-cut to build Towashi, the once-peaceful Jukai Forest " +
            "grew violently hostile to outsiders."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5c02102-3be7-462f-ab9c-ff404255002d.jpg?1783923846"
        ruling(
            "2022-02-18",
            "You choose the type of counter that each of the two green Spirit creature tokens will get separately."
        )
        ruling(
            "2022-02-18",
            "If a replacement effect causes you to create more than two tokens, you will make that choice for " +
                "each of those tokens individually. For example, if you resolve Invoke the Ancients while you " +
                "control Parallel Lives, you will choose a type of counter for each of the four resulting tokens."
        )
    }
}
