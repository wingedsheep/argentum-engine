package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shimian Night Stalker — Legends #116
 * {3}{B}{B} · Creature — Nightstalker · 4/4
 *
 * {B}, {T}: All damage that would be dealt to you this turn by target attacking creature is dealt
 * to this creature instead.
 *
 * A source-keyed, turn-long redirection shield: only that attacker's damage, only damage to you,
 * and it ends if either the attacker or the Night Stalker leaves the battlefield.
 */
val ShimianNightStalker = card("Shimian Night Stalker") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightstalker"
    power = 4
    toughness = 4
    oracleText = "{B}, {T}: All damage that would be dealt to you this turn by target attacking creature " +
        "is dealt to this creature instead."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val attacker = target(TargetFilter.AttackingCreature)
        effect = Effects.RedirectNextDamage(
            protectedTargets = listOf(EffectTarget.Controller),
            redirectTo = EffectTarget.Self,
            scope = RedirectScope.CONTINUOUS,
            fromSource = attacker
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Jesper Myrfors"
        flavorText = "\"'Tis now the very witching time of night,/ When churchyards yawn and hell itself " +
            "breathes out/ Contagion to this world.\" —William Shakespeare, Hamlet"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e04a1b9-c561-4e34-86d9-129ea0346631.jpg?1783948063"
    }
}
