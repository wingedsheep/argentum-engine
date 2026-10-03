package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skoa, Embermage — Modern Horizons 3 #138
 * {4}{R}{R} · Legendary Creature — Goblin Wizard · 4/4
 *
 * When Skoa enters, it deals 4 damage to any target.
 * Grandeur — Discard another card named Skoa, Embermage, Sacrifice two Mountains: Skoa deals 4
 * damage to any target.
 *
 * Grandeur is an ability word (no rules meaning). The discard draws from hand, so the Skoa on the
 * battlefield is never a candidate and "another" holds automatically (same shape as Page, Loose Leaf).
 * "Mountains" is the land subtype — nonbasic Mountains qualify.
 */
val SkoaEmbermage = card("Skoa, Embermage") {
    manaCost = "{4}{R}{R}"
    typeLine = "Legendary Creature — Goblin Wizard"
    power = 4
    toughness = 4
    oracleText = "When Skoa enters, it deals 4 damage to any target.\n" +
        "Grandeur — Discard another card named Skoa, Embermage, Sacrifice two Mountains: " +
        "Skoa deals 4 damage to any target."

    triggeredAbility {
        val anyTarget = target(Targets.Any)
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(4, anyTarget, damageSource = EffectTarget.Self)
    }

    activatedAbility {
        val anyTarget = target(Targets.Any)
        cost = Costs.Composite(
            Costs.Discard(GameObjectFilter.Any.named("Skoa, Embermage")),
            Costs.SacrificeMultiple(2, GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)),
        )
        effect = Effects.DealDamage(4, anyTarget, damageSource = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Kevin Sidharta"
        flavorText = "\"I need kindling. You'll do nicely.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf34b0f8-a68a-428f-9f2c-9556229367ec.jpg?1783911267"
    }
}
