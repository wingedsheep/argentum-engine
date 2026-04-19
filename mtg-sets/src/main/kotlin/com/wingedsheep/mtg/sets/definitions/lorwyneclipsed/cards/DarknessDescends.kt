package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Darkness Descends
 * {2}{B}{B}
 * Sorcery
 * Put two -1/-1 counters on each creature.
 */
val DarknessDescends = card("Darkness Descends") {
    manaCost = "{2}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Put two -1/-1 counters on each creature."

    spell {
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreatures,
            effect = AddCountersEffect(
                counterType = Counters.MINUS_ONE_MINUS_ONE,
                count = 2,
                target = EffectTarget.Self
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Ralph Horsley"
        flavorText = "Isilu's presence turned day to night, fellowship to suspicion, and neighbors against one another."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8aa3895-df83-4f8e-9e23-1a665614b662.jpg?1767863447"
    }
}
