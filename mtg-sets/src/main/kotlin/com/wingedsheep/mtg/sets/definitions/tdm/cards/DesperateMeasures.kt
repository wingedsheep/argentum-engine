package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Desperate Measures — Tarkir: Dragonstorm #78
 * {B} · Instant
 *
 * Target creature gets +1/-1 until end of turn. When it dies under your control this
 * turn, draw two cards.
 *
 * Modeled as the +1/-1 stat change plus a watched-entity delayed triggered ability
 * (`Triggers.Dies` scoped to the target via `watchedTarget`, expiring at end of turn).
 * This mirrors the Long River Lurker / Commando Raid "whenever that creature ... this
 * turn" pattern. The "under your control" clause is honored for the normal path (the
 * creature dies while you control it); the rare control-change-then-dies case is the
 * same watched-entity limitation shared by all such delayed triggers — the trigger is
 * scoped by entity id, not by re-checking last-known controller.
 */
val DesperateMeasures = card("Desperate Measures") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/-1 until end of turn. When it dies under your control this turn, draw two cards."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Composite(listOf(
            ModifyStatsEffect(1, -1, t),
            CreateDelayedTriggerEffect(
                effect = Effects.DrawCards(2),
                trigger = Triggers.Dies,
                watchedTarget = t,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Gaboleps"
        flavorText = "\"Sing for us when we depart for battle, and sing for us if we fail to return.\"\n—Mardu parting"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccbc6fd0-42bc-4e8b-96bc-69a631ba7106.jpg?1743204274"
        ruling("2025-04-04", "As long as you control it, you will get to draw two cards when the target creature goes to the graveyard, regardless of whether it died due to having 0 toughness or something else.")
        ruling("2025-04-04", "If the creature is no longer a creature at the time it goes to the graveyard, you will still draw two cards if you controlled it at that time.")
    }
}
