package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Verdant Outrider
 * {2}{G}
 * Creature — Human Knight
 * 4/2
 *
 * {1}{G}: This creature can't be blocked by creatures with power 2 or less this turn.
 *
 * Modelled as a durational grant of the printed-static [CantBeBlockedBy] restriction rather
 * than a bespoke effect: the blocking-legality check reads granted statics the same way it
 * reads printed ones, so power is re-evaluated continuously at declare-blockers (a creature
 * pumped above 2 after activation *can* block).
 */
val VerdantOutrider = card("Verdant Outrider") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Knight"
    oracleText = "{1}{G}: This creature can't be blocked by creatures with power 2 or less this turn."
    power = 4
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.GrantStaticAbility(
            ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Taras Susak"
        flavorText = "Some knights who survived the invasion have forsaken what remains of their courts. " +
            "The newly formed Verdant Order has sworn to defend the last untouched parts of the wilds."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c34830e4-823a-40fa-ba41-bb2afbf1e499.jpg?1783915074"
    }
}
