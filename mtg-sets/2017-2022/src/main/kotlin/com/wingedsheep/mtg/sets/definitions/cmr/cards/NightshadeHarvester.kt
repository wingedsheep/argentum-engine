package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nightshade Harvester {3}{B} — Creature — Elf Shaman 2/2
 *
 * Whenever a land an opponent controls enters, that player loses 1 life. Put a +1/+1 counter on
 * this creature.
 */
val NightshadeHarvester = card("Nightshade Harvester") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Shaman"
    power = 2
    toughness = 2
    oracleText = "Whenever a land an opponent controls enters, that player loses 1 life. " +
        "Put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.opponentControls()).enters()
        effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever a land an opponent controls enters, that player loses 1 life. " +
            "Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "PINDURSKI"
        flavorText = "\"Anywhere the moonlight touches—that's my garden.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8297ab13-d6f3-487b-86ec-6eb299eb0614.jpg?1783928832"
    }
}
