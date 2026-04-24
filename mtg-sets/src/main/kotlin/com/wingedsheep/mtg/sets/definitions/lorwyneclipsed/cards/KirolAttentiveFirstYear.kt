package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kirol, Attentive First-Year
 * {1}{R/W}{R/W}
 * Legendary Creature — Vampire Cleric
 * 3/3
 *
 * Tap two untapped creatures you control: Copy target triggered ability you control.
 * You may choose new targets for the copy. Activate only once each turn.
 */
val KirolAttentiveFirstYear = card("Kirol, Attentive First-Year") {
    manaCost = "{1}{R/W}{R/W}"
    typeLine = "Legendary Creature — Vampire Cleric"
    power = 3
    toughness = 3
    oracleText = "Tap two untapped creatures you control: Copy target triggered ability you control. You may choose new targets for the copy. Activate only once each turn."

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature)
        val ability = target("target triggered ability you control", Targets.TriggeredAbilityYouControl)
        effect = Effects.CopyTargetTriggeredAbility(ability)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Tap two untapped creatures you control: Copy target triggered ability you control. You may choose new targets for the copy. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Evyn Fong"
        flavorText = "Kirol hated being a pawn in Morcant's game. If dawnglove was as potent as she suggested, it should be preserved, not used to tip the balance of power."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c11cb0eb-819e-4905-ad95-e43618d3c81e.jpg?1765542087"
        ruling("2025-11-17", "If the ability copied by Kirol's ability is modal (that is, if it says, \"Choose one —\" or similar), the mode is copied and can't be changed.")
        ruling("2025-11-17", "If the ability copied by Kirol's ability divides damage or distributes counters among a number of targets, the division and number of targets can't be changed. If you choose new targets, you must choose the same number of targets.")
        ruling("2025-11-17", "Any choices made when the ability resolves won't have been made yet when it's copied by Kirol's ability. Any such choices will be made separately when the copy resolves. If a triggered ability asks you to pay a cost, you pay that cost for the copy separately.")
    }
}
