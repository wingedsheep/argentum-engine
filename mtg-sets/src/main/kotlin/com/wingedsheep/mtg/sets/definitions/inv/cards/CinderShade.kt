package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cinder Shade
 * {1}{B}{R}
 * Creature — Shade
 * 1/1
 * {B}: This creature gets +1/+1 until end of turn.
 * {R}, Sacrifice this creature: It deals damage equal to its power to target creature.
 */
val CinderShade = card("Cinder Shade") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Shade"
    power = 1
    toughness = 1
    oracleText = "{B}: This creature gets +1/+1 until end of turn.\n" +
        "{R}, Sacrifice this creature: It deals damage equal to its power to target creature."

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{R}"),
            Costs.SacrificeSelf
        )
        val t = target("target creature", Targets.Creature)
        // Power is read from the sacrifice-time snapshot (Rule 608.2h — "as it last
        // existed on the battlefield"), since the source has already left play.
        effect = Effects.DealDamage(DynamicAmounts.sacrificedPower(), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Nelson DeCastro"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8dd933a-19ed-4d30-a94a-bfb2f66f8f13.jpg?1562932184"
    }
}
