package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Explosive Welcome — Strixhaven: School of Mages #100 (canonical printing)
 * {7}{R} · Instant
 *
 * Explosive Welcome deals 5 damage to any target and 3 damage to any other target. Add {R}{R}{R}.
 *
 * The Boulder Dash two-target shape ([TargetOther] keeps the second target distinct from the
 * first), followed by [Effects.AddMana] for the three red.
 */
val ExplosiveWelcome = card("Explosive Welcome") {
    manaCost = "{7}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Explosive Welcome deals 5 damage to any target and 3 damage to any other target. " +
        "Add {R}{R}{R}."

    spell {
        val first = target(AnyTarget(descriptionOverride = "any target (takes 5 damage)"))
        val second = target(TargetOther(AnyTarget(descriptionOverride = "any other target (takes 3 damage)")))
        effect = Effects.DealDamage(5, first) then
            Effects.DealDamage(3, second) then
            Effects.AddMana(Color.RED, 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Mathias Kollros"
        flavorText = "Rowan and Will were a natural fit in Prismari College, the school of outrageous elemental arts."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/122c01e6-38a6-456e-971e-9004df85ac1c.jpg?1783927356"
    }
}
