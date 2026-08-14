package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Mind's Eye — Mirrodin #205
 * {5} · Artifact
 *
 * Whenever an opponent draws a card, you may pay {1}. If you do, draw a card.
 *
 * [Triggers.OpponentDraws] fires once per individual card drawn (CR 121.2), so an opponent's
 * "draw three cards" puts three separate instances on the stack and each is paid for — or
 * declined — on its own. The payment is a resolution-time optional cost
 * ([MayPayManaEffect] → `Gate.MayPay`), so the {1} is only asked for as each instance
 * resolves, and an empty mana pool with no untapped lands skips the prompt entirely rather
 * than offering an unpayable "yes".
 *
 * The replacement draw is a normal draw by the Eye's controller, so it feeds their own
 * draw triggers; it is not itself an opponent draw and can never re-trigger this ability.
 */
val MindsEye = card("Mind's Eye") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "Whenever an opponent draws a card, you may pay {1}. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.OpponentDraws
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Ideas drift like petals on the wind. I have only to lift my face to the breeze.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0899f753-c229-4cdf-a60c-4978a6506def.jpg?1783944513"
    }
}
