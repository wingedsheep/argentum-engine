package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Whelp
 * {2}{R}{R}
 * Creature — Dragon
 * 2/3
 * Flying
 * {R}: This creature gets +1/+0 until end of turn. If this ability has been activated four or
 * more times this turn, sacrifice this creature at the beginning of the next end step.
 *
 * The burnout clause reads a tally rather than imposing a limit, so the ability opts into per-turn
 * activation bookkeeping (`trackActivations`, counted at activation) and reads it back at
 * resolution. Per the rulings, the fourth and each later activation schedules its own delayed
 * sacrifice; an activation during the end step waits for the next turn's end step.
 */
val DragonWhelp = card("Dragon Whelp") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\n{R}: This creature gets +1/+0 until end of turn. If this ability has been " +
        "activated four or more times this turn, sacrifice this creature at the beginning of the next end step."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{R}")
        trackActivations = true
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then Effects.If(
            condition = Conditions.ThisAbilityActivatedThisTurnAtLeast(4),
            then = Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = SacrificeSelfEffect,
            )
        )
        description = "{R}: This creature gets +1/+0 until end of turn. If this ability has been activated four or more times this turn, sacrifice this creature at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Amy Weber"
        flavorText = "\"O to be a dragon . . . of silkworm size or immense . . .\"\n—Marianne Moore, \"O to Be a Dragon\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bbf1eab-bc32-4835-b566-8634b1fe81b0.jpg?1783948688"
        ruling("2022-12-08", "There's no limit to the number of times you can activate Dragon Whelp's ability in a turn. However, the fourth activation in a turn and each subsequent activation will cause a delayed triggered ability that will force you to sacrifice Dragon Whelp at the beginning of the next end step.")
        ruling("2022-12-08", "If the fourth (or subsequent) time Dragon Whelp's ability is activated during the same turn is during that turn's end step, the delayed triggered ability won't trigger until the beginning of the next turn's end step. You'll have to sacrifice Dragon Whelp at that time.")
    }
}
