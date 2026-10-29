package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Chain Veil
 * {4}
 * Legendary Artifact
 * At the beginning of your end step, if you didn't activate a loyalty ability of a planeswalker
 * this turn, you lose 2 life.
 * {4}, {T}: For each planeswalker you control, you may activate one of its loyalty abilities once
 * this turn as though none of its loyalty abilities have been activated this turn.
 */
val TheChainVeil = card("The Chain Veil") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "At the beginning of your end step, if you didn't activate a loyalty ability of a planeswalker this turn, you lose 2 life.\n{4}, {T}: For each planeswalker you control, you may activate one of its loyalty abilities once this turn as though none of its loyalty abilities have been activated this turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.YouActivatedLoyaltyAbilityThisTurn())
        effect = Effects.LoseLife(2, EffectTarget.Controller)
    }

    // A player-held grant, so it also covers planeswalkers that come under your control later
    // this turn; each resolution is one more activation (rulings 2014-07-18).
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.AdditionalLoyaltyActivations()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "215"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0415cc0e-979e-42cc-a56d-88d13153a7de.jpg?1783939159"
        ruling("2014-07-18", "For the first ability, it doesn't matter whether the planeswalker is still on the battlefield as your end step begins. If you activated one of its loyalty abilities that turn, The Chain Veil's triggered ability won't trigger.")
        ruling("2014-07-18", "Because the last ability modifies the rules of the game, it affects not only planeswalkers you control when it resolves, but also planeswalkers that come under your control later in the turn.")
        ruling("2014-07-18", "Each additional time The Chain Veil's last ability resolves will allow you to activate a loyalty ability of each planeswalker you control an additional time.")
    }
}
