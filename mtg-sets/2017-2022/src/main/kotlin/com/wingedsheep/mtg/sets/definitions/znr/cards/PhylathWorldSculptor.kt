package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Phylath, World Sculptor — Zendikar Rising #234
 * {4}{R}{G} · Legendary Creature — Elemental · 5/5
 *
 * When Phylath enters, create a 0/1 green Plant creature token for each basic land you control.
 * Landfall — Whenever a land you control enters, put four +1/+1 counters on target Plant you control.
 *
 * "Target Plant" is a bare subtype noun, so any Plant permanent you control is a legal target.
 */
val PhylathWorldSculptor = card("Phylath, World Sculptor") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Legendary Creature — Elemental"
    oracleText = "When Phylath enters, create a 0/1 green Plant creature token for each basic land you control.\n" +
        "Landfall — Whenever a land you control enters, put four +1/+1 counters on target Plant you control."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.BasicLand).count(),
            power = 0,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Plant"),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val t = target(TargetFilter.Permanent.withSubtype("Plant").youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 4, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7af88c9-70ca-484c-bddf-b705e0ea7bc7.jpg"
    }
}
