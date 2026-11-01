package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Carnival // Carnage (RNA 222) — split-layout spell (CR 709).
 *
 * Carnival {B/R} — Instant
 *   Carnival deals 1 damage to target creature or planeswalker and 1 damage to that permanent's controller.
 *
 * Carnage {2}{B}{R} — Sorcery
 *   Carnage deals 3 damage to target opponent. That player discards two cards.
 */
val CarnivalCarnage = card("Carnival // Carnage") {
    layout = CardLayout.SPLIT
    colorIdentity = "BR"

    face("Carnival") {
        manaCost = "{B/R}"
        typeLine = "Instant"
        oracleText = "Carnival deals 1 damage to target creature or planeswalker and 1 damage to that permanent's controller."

        spell {
            val permanent = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.DealDamage(1, permanent) then
                Effects.DealDamage(1, EffectTarget.TargetController)
        }
    }

    face("Carnage") {
        manaCost = "{2}{B}{R}"
        typeLine = "Sorcery"
        oracleText = "Carnage deals 3 damage to target opponent. That player discards two cards."

        spell {
            val opponent = target(Targets.Opponent)
            effect = Effects.DealDamage(3, opponent) then Effects.Discard(2, opponent)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1dbc559-c78c-4675-9582-9c28f8151bc7.jpg?1783933629"
        ruling(
            "2019-01-25",
            "If the target permanent is an illegal target by the time Carnival tries to resolve, the spell doesn't resolve. No player is dealt damage."
        )
        ruling(
            "2019-01-25",
            "If the target player is an illegal target by the time Carnage tries to resolve, the spell doesn't resolve. No player discards."
        )
    }
}
