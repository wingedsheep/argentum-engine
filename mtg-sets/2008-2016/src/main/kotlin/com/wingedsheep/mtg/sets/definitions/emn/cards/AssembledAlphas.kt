package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Assembled Alphas (Eldritch Moon #117)
 * {5}{R}
 * Creature — Wolf
 * 5/5
 * Whenever this creature blocks or becomes blocked by a creature, this creature deals 3 damage to
 * that creature and 3 damage to that creature's controller.
 *
 * "By **a** creature" fires once per combat partner, so `blocksOrBecomesBlocked` takes an explicit
 * filter (which turns `oncePerCombat` off) and the partner is the triggering entity. The controller
 * half reads the partner's last-known controller when it has left the battlefield, so the player
 * is still dealt 3 even if the creature is gone or its damage is prevented.
 */
val AssembledAlphas = card("Assembled Alphas") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    power = 5
    toughness = 5
    oracleText = "Whenever this creature blocks or becomes blocked by a creature, this creature deals " +
        "3 damage to that creature and 3 damage to that creature's controller."

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(GameObjectFilter.Creature)
        effect = Effects.DealDamage(3, EffectTarget.TriggeringEntity) then
            Effects.DealDamage(3, EffectTarget.ControllerOfTriggeringEntity)
        description = "Whenever this creature blocks or becomes blocked by a creature, this creature " +
            "deals 3 damage to that creature and 3 damage to that creature's controller."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Christopher Moeller"
        flavorText = "There is always a toll to pay for passage."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8603c493-afb0-47a1-b4e3-9848b3824840.jpg?1783937466"
        ruling(
            "2016-07-13",
            "The triggered ability deals damage to the creature's controller even if the creature isn't " +
                "dealt damage, perhaps because it has left the battlefield or because the damage was prevented.",
        )
        ruling(
            "2016-07-13",
            "The triggered ability triggers once for each creature blocking or blocked by Assembled Alphas. " +
                "The ability resolves and deals damage to that creature before combat damage is dealt. If that " +
                "damage destroys all creatures blocking Assembled Alphas, Assembled Alphas doesn't become unblocked.",
        )
    }
}
