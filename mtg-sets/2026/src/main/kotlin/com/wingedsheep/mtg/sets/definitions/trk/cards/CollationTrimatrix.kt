package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Collation Trimatrix
 * {B}
 * Artifact Creature — Borg
 * 1/1
 *
 * Tap three untapped artifact creatures you control: You draw a card and lose 1 life.
 */
val CollationTrimatrix = card("Collation Trimatrix") {
    manaCost = "{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    oracleText = "Tap three untapped artifact creatures you control: You draw a card and lose 1 life."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.TapPermanents(3, GameObjectFilter.ArtifactCreature)
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Bryan Sola"
        flavorText = "Every new species the Borg encounter holds potential to improve the entire Collective. " +
            "Its culture is analyzed, its technology is studied, and its flesh is disassembled."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92c19351-0592-4269-90f4-2475d5529400.jpg?1793359423"
    }
}
