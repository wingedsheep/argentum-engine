package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Burn, Burn, Tree and Fern
 * {3}{R}
 * Enchantment — Saga
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — This Saga deals 6 damage to target creature an opponent controls.
 * II — Destroy target artifact an opponent controls.
 * III, IV — Add {R}.
 *
 * Three plain chapters. Chapters I and II each declare their own target, chosen fresh as that
 * chapter ability goes on the stack, so either can fizzle on its own without affecting the rest of
 * the Saga. Chapters III and IV are the same ability declared twice — the engine keys chapter
 * abilities by lore-counter number, so a shared "III, IV" line is two `sagaChapter` blocks.
 *
 * The mana from III / IV lands in the controller's pool during their precombat main phase (the
 * chapter trigger resolves there, CR 714.3c) and empties as that step ends like any other mana.
 */
val BurnBurnTreeAndFern = card("Burn, Burn, Tree and Fern") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — This Saga deals 6 damage to target creature an opponent controls.\n" +
        "II — Destroy target artifact an opponent controls.\n" +
        "III, IV — Add {R}."

    // I — This Saga deals 6 damage to target creature an opponent controls.
    sagaChapter(1) {
        val creature = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.DealDamage(6, creature)
    }

    // II — Destroy target artifact an opponent controls.
    sagaChapter(2) {
        val artifact = target(
            "target artifact an opponent controls",
            TargetPermanent(filter = TargetFilter.Artifact.opponentControls())
        )
        effect = Effects.Destroy(artifact)
    }

    // III, IV — Add {R}.
    sagaChapter(3) {
        effect = Effects.AddMana(Color.RED, 1)
    }
    sagaChapter(4) {
        effect = Effects.AddMana(Color.RED, 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fceb1a2d-121e-49ad-acf2-1bb5aebec116.jpg?1784376970"
    }
}
