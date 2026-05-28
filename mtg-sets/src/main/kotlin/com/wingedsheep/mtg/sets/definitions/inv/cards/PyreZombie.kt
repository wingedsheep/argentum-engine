package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pyre Zombie
 * {1}{B}{R}
 * Creature — Zombie (2/1)
 *
 * At the beginning of your upkeep, if this card is in your graveyard, you may pay {1}{B}{B}.
 * If you do, return it to your hand.
 * {1}{R}{R}, Sacrifice this creature: It deals 2 damage to any target.
 *
 * The recursion ability functions from the graveyard via `triggerZone = Zone.GRAVEYARD`
 * (intervening-if + MayPayMana), the same shape as Onslaught's Gigapede.
 */
val PyreZombie = card("Pyre Zombie") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "At the beginning of your upkeep, if this card is in your graveyard, you may pay {1}{B}{B}. " +
        "If you do, return it to your hand.\n" +
        "{1}{R}{R}, Sacrifice this creature: It deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}{B}{B}"),
            effect = Effects.ReturnToHand(EffectTarget.Self),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}{R}"), Costs.SacrificeSelf)
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Nelson DeCastro"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c030108-2995-4fb0-9b80-efdfdd0f11e0.jpg?1562916671"
    }
}
