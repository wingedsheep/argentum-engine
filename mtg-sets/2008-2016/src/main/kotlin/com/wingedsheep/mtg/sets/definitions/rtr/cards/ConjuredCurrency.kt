package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Conjured Currency
 * {5}{U}
 * Enchantment
 * At the beginning of your upkeep, you may exchange control of this enchantment and target
 * permanent you neither own nor control.
 *
 * "You neither own nor control" is `Not(OwnedByYou) and Not(ControlledByYou)` rather than
 * `ownedByOpponent().opponentControls()` — a teammate's permanent counts too (see rulings).
 */
private val permanentYouNeitherOwnNorControl = TargetFilter(
    GameObjectFilter.Permanent.withControllerPredicate(
        ControllerPredicate.And(
            listOf(
                ControllerPredicate.Not(ControllerPredicate.OwnedByYou),
                ControllerPredicate.Not(ControllerPredicate.ControlledByYou),
            )
        )
    )
)

val ConjuredCurrency = card("Conjured Currency") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may exchange control of this enchantment and target permanent you neither own nor control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        description = "You may exchange control of this enchantment and target permanent you neither own nor control."
        val t = target(permanentYouNeitherOwnNorControl)
        effect = Effects.ExchangeControl(EffectTarget.Self, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Steve Argyle"
        flavorText = "A bargain in Keyhole Downs is always too good to be true."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7c2ea6e-7d29-4526-b135-9bbb1eed9d4a.jpg?1783940370"
        ruling("2012-10-01", "The only legal target for Conjured Currency's ability is a permanent both owned and controlled by another player, although it doesn't have to be the same player. For example, a permanent owned by an opponent but controlled by your teammate (or another opponent) could be chosen as the target.")
        ruling("2012-10-01", "The exchange occurs only if Conjured Currency and the target permanent are both on the battlefield and controlled by different players when the ability resolves. In addition, the target permanent must be one you neither own nor control. If any of these things aren't true when the ability tries to resolve, the exchange won't happen and neither permanent will change controllers.")
        ruling("2012-10-01", "You don't necessarily have to control Conjured Currency when its ability resolves in order for the exchange to happen. If another player has gained control of Conjured Currency while the ability is on the stack, and a third player controls the target (and you don't own it), you may choose to have those two players exchange control of the relevant permanents.")
    }
}
