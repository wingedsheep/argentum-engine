package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Emergency Holographic Crew
 * {3}{U}
 * Artifact Creature — Illusion Officer
 * 3/4
 *
 * When this creature enters, choose one —
 * • Return target Spacecraft card from your graveyard to your hand.
 * • Surveil 2.
 */
val EmergencyHolographicCrew = card("Emergency Holographic Crew") {
    manaCost = "{3}{U}"
    typeLine = "Artifact Creature — Illusion Officer"
    colorIdentity = "U"
    oracleText = "When this creature enters, choose one —\n• Return target Spacecraft card from your graveyard to your hand.\n• Surveil 2. (Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)"
    power = 3
    toughness = 4
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(listOf(
            mode("Return target Spacecraft card from your graveyard to your hand") {
                val spacecraft = target(
                    TargetFilter(GameObjectFilter.Any.withSubtype("Spacecraft").ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = Effects.ReturnToHand(spacecraft)
            },
            mode("Surveil 2") { effect = Effects.Surveil(2) }
        ))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Thanh Tuấn"
        flavorText = "\"Reporting for duty, Captain.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53943948-8f24-495a-b6eb-e788fa169fcd.jpg?1793359393"
    }
}
