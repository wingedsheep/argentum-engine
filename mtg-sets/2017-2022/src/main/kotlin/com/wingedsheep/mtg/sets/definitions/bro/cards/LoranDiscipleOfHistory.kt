package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Loran, Disciple of History — The Brothers' War #13
 * {3}{W} · Legendary Creature — Human Artificer · 3/3
 *
 * Whenever Loran or another legendary creature you control enters, return target artifact card
 * from your graveyard to your hand.
 *
 * Loran is itself a legendary creature, so "Loran or another legendary creature you control"
 * is exactly the subject `a legendary creature you control` (the Frodo Baggins shape).
 */
val LoranDiscipleOfHistory = card("Loran, Disciple of History") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Artificer"
    power = 3
    toughness = 3
    oracleText = "Whenever Loran or another legendary creature you control enters, return target " +
        "artifact card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.legendary().youControl()).enters()
        val t = target(TargetFilter.ArtifactInYourGraveyard)
        effect = Effects.Move(t, Zone.HAND)
        description = "Whenever Loran or another legendary creature you control enters, return " +
            "target artifact card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Cristi Balanescu"
        flavorText = "Urza and Mishra may have been Tocasia's most famous students, but Loran was " +
            "the true heir to her legacy."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9998f898-1f08-481b-8505-b890f0fec5e0.jpg?1783920129"
    }
}
