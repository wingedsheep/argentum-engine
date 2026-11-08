package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phantasmal Shieldback — Foundations Jumpstart #8
 * {U} · Creature — Turtle Illusion · 1/3
 *
 * When this creature becomes the target of a spell or ability, sacrifice it.
 * When this creature dies, draw a card.
 */
val PhantasmalShieldback = card("Phantasmal Shieldback") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Turtle Illusion"
    power = 1
    toughness = 3
    oracleText = "When this creature becomes the target of a spell or ability, sacrifice it.\n" +
        "When this creature dies, draw a card."

    triggeredAbility {
        trigger = Triggers.self.becomesTarget()
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Daniel Ljunggren"
        flavorText = "It materialized in the mist, and the invading horde broke like waves against solid rock."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c28ae5c-e773-43bd-a328-c3d6046d24be.jpg?1783908869"
    }
}
