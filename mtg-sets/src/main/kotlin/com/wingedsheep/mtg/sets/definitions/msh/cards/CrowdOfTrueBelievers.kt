package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Crowd of True Believers — Marvel Super Heroes #14 (common)
 * {W} · Creature — Human Citizen · 1/2
 *
 * {T}: Target creature you control that's attacking alone gets +1/+0 until end of turn.
 * You gain 1 life.
 *
 * "Attacking alone" is CR 506.5 — *a creature is attacking alone if it's attacking but no other
 * creatures are* — and it lives entirely in the **target filter**
 * ([TargetFilter].Creature.youControl().attackingAlone()), because it is a targeting restriction.
 * That placement is the point: CR 608.2b re-checks every target on resolution, so if a second
 * creature starts attacking while the ability is on the stack (an effect that puts a creature onto
 * the battlefield attacking), the chosen target stops being legal and the ability is countered for
 * having no legal targets — which is what the printed card does. Splitting the clause into an
 * attacking-only target plus an [ActivationRestriction] on the global attacker count reads the same
 * at activation time but silently resolves in that case, because activation restrictions are
 * consulted once, when the ability is activated.
 */
val CrowdOfTrueBelievers = card("Crowd of True Believers") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Citizen"
    power = 1
    toughness = 2
    oracleText = "{T}: Target creature you control that's attacking alone gets +1/+0 until end " +
        "of turn. You gain 1 life."

    activatedAbility {
        cost = Costs.Tap
        val attacker = target(
            "target creature you control that's attacking alone",
            TargetCreature(filter = TargetFilter.Creature.youControl().attackingAlone()),
        )
        effect = Effects.Composite(
            Effects.ModifyStats(1, 0, attacker),
            Effects.GainLife(1),
        )
        description = "{T}: Target creature you control that's attacking alone gets +1/+0 until " +
            "end of turn. You gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Michele Giorgi"
        flavorText = "Being a hero is easy when you know who you're fighting for."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b2fffb8-d538-4772-8fbc-9bec3b9c4d9c.jpg?1783902974"
    }
}
