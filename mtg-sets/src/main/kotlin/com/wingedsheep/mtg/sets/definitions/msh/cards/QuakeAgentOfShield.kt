package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Quake, Agent of S.H.I.E.L.D. — Marvel Super Heroes #32
 * {2}{W} · Legendary Creature — Inhuman Spy Hero · 3/3
 *
 * Seismic Takedown — Whenever you cast a noncreature spell, tap target creature or land.
 *
 * Modeling notes:
 *  - "Seismic Takedown" is a flavor ability name only; it carries no rules meaning, so it lives
 *    in the oracle text and the trigger's description, and nowhere else.
 *  - [Triggers.YouCastNoncreature] fires on the cast, so the trigger goes on the stack *above*
 *    the spell that caused it and resolves first (CR 603.3b) — the tap happens before the
 *    noncreature spell resolves.
 *  - The union "creature or land" target is the pre-built [TargetFilter.CreatureOrLandPermanent]
 *    (Lava Flow / Befoul), not two separate requirements: it's one target that may be either.
 */
val QuakeAgentOfShield = card("Quake, Agent of S.H.I.E.L.D.") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Inhuman Spy Hero"
    power = 3
    toughness = 3
    oracleText = "Seismic Takedown — Whenever you cast a noncreature spell, tap target creature or land."

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        val shaken = target(
            "target creature or land",
            TargetPermanent(filter = TargetFilter.CreatureOrLandPermanent)
        )
        effect = Effects.Tap(shaken)
        description = "Seismic Takedown — Whenever you cast a noncreature spell, tap target creature or land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Solan"
        flavorText = "\"Let's see you drive away without a road.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92dad216-ef8e-4af2-a3c6-1d215721c478.jpg?1783902967"
    }
}
