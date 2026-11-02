package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Response // Resurgence (GRN 229) — split-layout spell (CR 709).
 *
 * Response {R/W}{R/W} — Instant
 *   Response deals 5 damage to target attacking or blocking creature.
 *
 * Resurgence {3}{R}{W} — Sorcery
 *   Creatures you control gain first strike and vigilance until end of turn. After this main
 *   phase, there is an additional combat phase followed by an additional main phase.
 */
val ResponseResurgence = card("Response // Resurgence") {
    layout = CardLayout.SPLIT
    colorIdentity = "RW"

    face("Response") {
        manaCost = "{R/W}{R/W}"
        typeLine = "Instant"
        oracleText = "Response deals 5 damage to target attacking or blocking creature."

        spell {
            val creature = target(TargetFilter.AttackingOrBlockingCreature)
            effect = Effects.DealDamage(5, creature)
        }
    }

    face("Resurgence") {
        manaCost = "{3}{R}{W}"
        typeLine = "Sorcery"
        oracleText = "Creatures you control gain first strike and vigilance until end of turn. " +
            "After this main phase, there is an additional combat phase followed by an additional main phase."

        spell {
            effect = Patterns.Group.grantKeywordToAll(Keyword.FIRST_STRIKE, Filters.Group.creaturesYouControl) then
                Patterns.Group.grantKeywordToAll(Keyword.VIGILANCE, Filters.Group.creaturesYouControl) then
                Effects.AddCombatPhase then
                Effects.AddMainPhase
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92162888-35ea-4f4f-ab99-64dd3104e230.jpg?1783934110"
    }
}
