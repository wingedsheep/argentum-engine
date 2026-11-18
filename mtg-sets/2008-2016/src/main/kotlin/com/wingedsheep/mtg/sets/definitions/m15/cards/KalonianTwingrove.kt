package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kalonian Twingrove
 * {5}{G}
 * Creature — Treefolk Warrior
 * * / *
 *
 * Kalonian Twingrove's power and toughness are each equal to the number of Forests you control.
 * When this creature enters, create a green Treefolk Warrior creature token with "This token's
 * power and toughness are each equal to the number of Forests you control."
 *
 * Both P/Ts are characteristic-defining abilities that recompute continuously: the Twingrove's own
 * via `dynamicStats` (works in every zone), the token's as a Layer 7b
 * [SetBasePowerToughnessDynamicStatic] on the token itself — the Voice of Resurgence shape — so it
 * tracks the Forest count rather than snapshotting it at creation.
 */
private val forestsYouControl = DynamicAmounts.landsWithSubtype(Subtype.FOREST)

val KalonianTwingrove = card("Kalonian Twingrove") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk Warrior"
    power = 0
    toughness = 0
    oracleText = "Kalonian Twingrove's power and toughness are each equal to the number of Forests you control.\n" +
        "When this creature enters, create a green Treefolk Warrior creature token with \"This token's " +
        "power and toughness are each equal to the number of Forests you control.\""

    dynamicStats(forestsYouControl)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Treefolk", "Warrior"),
            staticAbilities = listOf(
                SetBasePowerToughnessDynamicStatic(
                    power = forestsYouControl,
                    toughness = forestsYouControl,
                    filter = GroupFilter.source(),
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/2/5/2569593a-d2f2-414c-9e61-2c34e8a5832d.jpg?1783939140"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dabe0865-5420-463e-9138-ccd805be8b31.jpg?1783939166"
        ruling("2014-07-18", "The ability that defines Kalonian Twingrove's power and toughness works in all zones, not just the battlefield.")
        ruling("2014-07-18", "The power and toughness of both Kalonian Twingrove and the token it creates will change as the number of Forests you control changes.")
    }
}
