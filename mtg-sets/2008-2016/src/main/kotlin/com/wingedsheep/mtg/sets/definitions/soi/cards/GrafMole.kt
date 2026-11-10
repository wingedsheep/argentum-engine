package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Graf Mole (SOI #207)
 * {2}{G}
 * Creature — Mole Beast
 * 2/4
 *
 * Whenever you sacrifice a Clue, you gain 3 life.
 *
 * "Sacrifice a Clue" is the per-permanent template (`Triggers.you.sacrifices`, CR 603.2c) keyed on the
 * Clue artifact subtype, so any Clue artifact sacrificed for any reason counts, and cracking two Clues
 * at once triggers twice — the same shape as [FleetingMemories].
 */
val GrafMole = card("Graf Mole") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Mole Beast"
    power = 2
    toughness = 4
    oracleText = "Whenever you sacrifice a Clue, you gain 3 life."

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Clue"))
        effect = Effects.GainLife(3)
        description = "Whenever you sacrifice a Clue, you gain 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Lars Grant-West"
        flavorText = "\"Their keen sense of smell draws them to the unnatural. To find one is a boon.\"\n" +
            "—Veka, hinterland inspector"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25a40334-65d8-46d2-9c56-389e9b32107c.jpg?1783937731"
    }
}
