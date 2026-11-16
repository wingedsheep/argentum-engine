package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Surge of Righteousness
 * {1}{W}
 * Instant
 * Destroy target black or red creature that's attacking or blocking. You gain 2 life.
 */
val SurgeOfRighteousness = card("Surge of Righteousness") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target black or red creature that's attacking or blocking. You gain 2 life."

    spell {
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.attackingOrBlocking().withAnyColor(Color.BLACK, Color.RED)
            )
        )
        effect = Effects.Destroy(t) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Marco Nelor"
        flavorText = "Though she stood alone, she struck with the force of an army."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/114366f3-237f-4f96-b644-5bd82d97b18b.jpg?1783938611"
        ruling("2015-02-25", "If Surge of Righteousness resolves but the creature isn't destroyed (perhaps because it regenerated or it has indestructible), you'll still gain 2 life.")
        ruling("2015-02-25", "However, if the creature becomes an illegal target before Surge of Righteousness resolves, it won't resolve and none of its effects will happen. You won't gain life.")
    }
}
