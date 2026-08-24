package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rainbow Vale
 * Land
 * {T}: Add one mana of any color. An opponent gains control of this land at the beginning of the
 * next end step.
 *
 * Still a mana ability (CR 605.1a — it adds mana, targets nothing, and isn't a loyalty ability),
 * so it never uses the stack and the delayed trigger is scheduled the instant it is activated.
 * The land stays yours for the rest of the turn; the control change lands at the next end step.
 */
val RainbowVale = card("Rainbow Vale") {
    typeLine = "Land"
    oracleText = "{T}: Add one mana of any color. An opponent gains control of this land at the " +
        "beginning of the next end step."

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.Composite(
            Effects.AddAnyColorMana(1),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = GiveControlToTargetPlayerEffect(
                    permanent = EffectTarget.Self,
                    newController = EffectTarget.PlayerRef(Player.AnOpponent),
                )
            )
        )
        description = "{T}: Add one mana of any color. An opponent gains control of this land at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Kaja Foglio"
        flavorText = "In the feudal days of Icatia, finding the Rainbow Vale was often the goal of Knights' quests."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1b138e1-f8fc-435c-9aed-98004768479c.jpg?1783947877"
    }
}
