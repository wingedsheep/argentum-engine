package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hypothesizzle
 * {3}{U}{R}
 * Instant
 *
 * Draw two cards. Then you may discard a nonland card. When you do, Hypothesizzle deals 4 damage
 * to target creature.
 *
 * "When you do" is a reflexive triggered ability (CR 603.12), Dokuchi Silencer's shape: the
 * optional action is discarding a nonland card, and only once it's performed does the damage
 * ability go on the stack and pick its target. No `damageSource = Self` — the spell has left the
 * stack by the time the reflexive ability resolves (Glacial Dragonhunt's note).
 */
val Hypothesizzle = card("Hypothesizzle") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Draw two cards. Then you may discard a nonland card. When you do, Hypothesizzle " +
        "deals 4 damage to target creature."

    spell {
        effect = Effects.Pipeline {
            run(Effects.DrawCards(2))
            run(Effects.ReflexiveTrigger(
                action = Patterns.Hand.discardCards(1, filter = GameObjectFilter.Nonland),
                optional = true,
                descriptionOverride = "You may discard a nonland card. When you do, Hypothesizzle " +
                    "deals 4 damage to target creature."
            ) {
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(4, creature)
            })
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Chris Seaman"
        flavorText = "\"It's like blowing up an arcane library in a thermobaric explosion. But in a " +
            "good way.\"\n—Bori Andon, Izzet blastseeker"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5690329-feb8-49c0-8f59-ee13782e8d3b.jpg?1783934131"
        ruling(
            "2024-04-12",
            "You don't choose a target creature at the time you cast Hypothesizzle. Rather, a second " +
                "\"reflexive\" ability triggers when you discard a nonland card. You choose a target for " +
                "that ability as it goes on the stack. Each player may respond to this triggered ability " +
                "as normal. Notably, the triggered ability isn't a crime, but the reflexive triggered " +
                "ability potentially is."
        )
    }
}
