package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Soulblast — Champions of Kamigawa #190 (canonical printing; reprinted in 10E)
 * {3}{R}{R}{R} · Instant
 *
 * As an additional cost to cast this spell, sacrifice all creatures you control.
 * Soulblast deals damage to any target equal to the total power of the sacrificed creatures.
 *
 * The cost sacrifices every creature you control with nothing chosen (`SacrificeAll`), and each is
 * snapshotted as it last existed on the battlefield, so the damage reads their power after pumps
 * and anthems even though they are in the graveyard by the time Soulblast resolves. Controlling no
 * creatures pays the cost for free and deals 0.
 */
val Soulblast = card("Soulblast") {
    manaCost = "{3}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice all creatures you control.\n" +
        "Soulblast deals damage to any target equal to the total power of the sacrificed creatures."

    additionalCost(Costs.additional.SacrificeAll(GameObjectFilter.Creature))

    spell {
        val target = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.totalPowerSacrificedThisWay(), target)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92fe38cc-2b12-4491-8deb-fbcb306febf2.jpg?1783944294"
    }
}
