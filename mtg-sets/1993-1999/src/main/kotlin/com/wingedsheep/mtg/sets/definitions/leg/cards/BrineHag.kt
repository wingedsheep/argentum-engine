package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brine Hag — Legends #49
 * {2}{U}{U} · Creature — Hag · 2/2
 *
 * When this creature dies, the base power and toughness of each creature that dealt damage to it
 * this turn become 0/2. (This effect lasts indefinitely.)
 *
 * "Each creature that dealt damage to it this turn" reads each candidate's own record of what it
 * damaged ([GameObjectFilter.dealtDamageToSourceThisTurn]), so it still answers after the Hag has
 * died. The base-P/T change is layer 7b and permanent.
 */
val BrineHag = card("Brine Hag") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Hag"
    power = 2
    toughness = 2
    oracleText = "When this creature dies, the base power and toughness of each creature that dealt " +
        "damage to it this turn become 0/2. (This effect lasts indefinitely.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.dealtDamageToSourceThisTurn()),
            Effects.SetBasePowerAndToughness(0, 2, EffectTarget.IterationEntity, Duration.Permanent)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a1e7796-fbfb-4976-879f-bb748429d5c7.jpg?1783948077"
    }
}
