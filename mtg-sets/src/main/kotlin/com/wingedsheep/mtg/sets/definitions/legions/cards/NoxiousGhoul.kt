package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Noxious Ghoul
 * {3}{B}{B}
 * Creature — Zombie
 * 3/3
 * Whenever Noxious Ghoul or another Zombie enters, all non-Zombie creatures get -1/-1 until end of turn.
 */
val NoxiousGhoul = card("Noxious Ghoul") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 3
    oracleText = "Whenever Noxious Ghoul or another Zombie enters, all non-Zombie creatures get -1/-1 until end of turn."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.ModifyStatsForAll(
            power = -1,
            toughness = -1,
            filter = GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.ZOMBIE))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Luca Zontini"
        flavorText = "Plague and death wrapped in one convenient package."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9d3b33d-25b4-42b4-a93e-2a6b69832030.jpg?1562945384"
    }
}
