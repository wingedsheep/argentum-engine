package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Deadly Riposte
 * {1}{W}
 * Instant
 * Deadly Riposte deals 3 damage to target tapped creature and you gain 2 life.
 *
 * Hand-corrected against the printed oracle text: the mtgish IR this card was generated from
 * records the life clause as `GainLife 3`, so the emitted draft gained 3 life instead of 2.
 * Don't regenerate this file from the corpus without re-checking that clause.
 */
val DeadlyRiposte = card("Deadly Riposte") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Deadly Riposte deals 3 damage to target tapped creature and you gain 2 life."
    spell {
        val tappedCreature = target("target", TargetCreature(filter = TargetFilter.Creature.tapped()))
        effect = Effects.Composite(
            Effects.DealDamage(3, tappedCreature),
            Effects.GainLife(2)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Olena Richards"
        flavorText = "\"Automatons are fast, precise, and tireless, but they all share one weakness: predictability. Learn their patterns. Strike when they're vulnerable.\"\n—Horance, Urzan general"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38eca0ae-d400-4afb-9a45-7100f4cd7149.jpg"
    }
}
