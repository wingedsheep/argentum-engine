package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plasm Capture
 * {G}{G}{U}{U}
 * Instant
 * Counter target spell. At the beginning of your next first main phase, add X mana in any
 * combination of colors, where X is that spell's mana value.
 *
 * Mana Drain's shape: the delayed trigger is created *before* the counter, while the target is
 * still on the stack, and `CreateDelayedTriggerExecutor` freezes the mana amount at creation.
 * "Your next first main phase" is exactly [Step.PRECOMBAT_MAIN] on your turn (2013-04-15 ruling:
 * not any other main phase), so cast during your own first main phase it waits for next turn's.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val PlasmCapture = card("Plasm Capture") {
    manaCost = "{G}{G}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Counter target spell. At the beginning of your next first main phase, add X mana in any " +
        "combination of colors, where X is that spell's mana value."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CreateDelayedTrigger(
            step = Step.PRECOMBAT_MAIN,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            effect = Effects.AddManaInAnyCombination(DynamicAmounts.manaValueOf(spell)),
        ) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0ffe8485-d5fb-47cc-af53-6e0fd062b7a2.jpg?1783940025"
        ruling(
            "2013-04-15",
            "If the countered spell had {X} in its mana cost, include the value chosen for that X when " +
                "determining how much mana to produce. For example, if you counter a spell that costs {X}{R} " +
                "with X equal to 5, you'll produce six mana."
        )
        ruling(
            "2013-04-15",
            "Your precombat main phase is the main phase that happens immediately after your draw step, not " +
                "any other main phase."
        )
    }
}
