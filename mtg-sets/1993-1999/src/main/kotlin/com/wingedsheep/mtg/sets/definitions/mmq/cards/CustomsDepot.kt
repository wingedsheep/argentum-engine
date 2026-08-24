package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Customs Depot
 * {1}{U}
 * Enchantment
 *
 * Whenever you cast a creature spell, you may pay {1}. If you do, draw a card, then discard a
 * card.
 *
 * "You may pay {1}. If you do, …" is an *optional cost rider on the triggered ability itself*
 * ([MayPayManaEffect] → `Gate.MayPay`), not a reflexive trigger: the payment and the payoff both
 * happen as this one ability resolves. Lightning Rift and Mind's Eye are the same shape.
 * Affordability is checked before prompting, so a tapped-out controller is never offered an
 * unpayable "yes".
 *
 * "Draw a card, then discard a card" is [Patterns.Hand].loot — draw, then the
 * gather → select → move discard pipeline, in that order, so the drawn card is a legal discard.
 */
val CustomsDepot = card("Customs Depot") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a creature spell, you may pay {1}. If you do, draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Patterns.Hand.loot(draw = 1, discard = 1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Scott M. Fischer"
        flavorText = "A bribe is always faster than filling out paperwork."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/067d8c46-c334-4b00-af06-2e28b6086c58.jpg"
    }
}
