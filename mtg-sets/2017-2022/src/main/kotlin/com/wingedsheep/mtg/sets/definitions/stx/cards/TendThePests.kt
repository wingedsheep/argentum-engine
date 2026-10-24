package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tend the Pests — Strixhaven: School of Mages #242 (canonical printing)
 * {B}{G} · Instant
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Create X 1/1 black and green Pest creature tokens with "When this token dies, you gain 1 life,"
 * where X is the sacrificed creature's power.
 *
 * Final Strike's sacrifice-as-additional-cost, with X read as [DynamicAmounts.sacrificedPower] —
 * the creature's last-known power as it was sacrificed — feeding the set's predefined Pest token
 * via the dynamic-count [Effects.CreatePredefinedToken]. A copy keeps the original's sacrificed
 * snapshot (CR 707.10), so it makes the same number of Pests without a fresh sacrifice (ruling).
 */
val TendThePests = card("Tend the Pests") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Create X 1/1 black and green Pest creature tokens with \"When this token dies, you gain " +
        "1 life,\" where X is the sacrificed creature's power."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.CreatePredefinedToken("Pest", DynamicAmounts.sacrificedPower())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "David Auden Nash"
        flavorText = "\"I've got a special treat for you today.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f43ed93-008d-44db-8204-ef2cc3b7cf8a.jpg?1783927287"
        ruling("2021-04-16", "Copies of Tend the Pests will use the power of the creature sacrificed for the original spell to determine the number of Pests each copy creates. Because the copies aren't cast, you won't sacrifice additional creatures for them.")
    }
}
