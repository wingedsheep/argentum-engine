package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Death Frenzy
 * {3}{B}{G}
 * Sorcery
 * All creatures get -2/-2 until end of turn. Whenever a creature dies this turn, you gain 1 life.
 */
val DeathFrenzy = card("Death Frenzy") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "All creatures get -2/-2 until end of turn. Whenever a creature dies this turn, you gain 1 life."

    spell {
        effect = Effects.Composite(
            listOf(
                GroupPatterns.modifyStatsForAll(-2, -2, GroupFilter.AllCreatures),
                Effects.CreateGlobalTriggeredAbility(
                    duration = Duration.EndOfTurn,
                    ability = TriggeredAbility.create(
                        trigger = Triggers.AnyCreatureDies.event,
                        binding = Triggers.AnyCreatureDies.binding,
                        effect = Effects.GainLife(1, EffectTarget.Controller)
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Steve Prescott"
        flavorText = "The crocodiles' putrid jaws swallow everything but the screams."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92096311-a3fa-41fc-b7a9-71ac2310f7fe.jpg?1562790443"
    }
}
