package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Meticulous Excavation
 * {W}
 * Enchantment
 * {2}{W}: Return target permanent you control to its owner's hand. If it has unearth, instead
 * exile it, then return that card to its owner's hand. Activate only during your turn.
 *
 * "Has unearth" is `withKeyword(UNEARTH)`, which the `unearth(cost)` builder adds to the card's
 * base keywords and the projection carries onto the battlefield (so a permanent that lost all its
 * abilities no longer has unearth). Exiling first is the point: an unearthed permanent's
 * "if it would leave the battlefield, exile it instead" replacement would turn a plain bounce into
 * exile, but a move that is itself an exile succeeds, and the card then goes from exile to hand.
 */
val MeticulousExcavation = card("Meticulous Excavation") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{2}{W}: Return target permanent you control to its owner's hand. If it has unearth, " +
        "instead exile it, then return that card to its owner's hand. Activate only during your turn."

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        val permanent = target(TargetFilter.PermanentYouControl)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(Filters.Permanent.withKeyword(Keyword.UNEARTH), permanent),
            then = Effects.Exile(permanent) then Effects.ReturnToHand(permanent),
            otherwise = Effects.ReturnToHand(permanent),
        )
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        description = "{2}{W}: Return target permanent you control to its owner's hand. If it has " +
            "unearth, instead exile it, then return that card to its owner's hand. Activate only during your turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Lucas Graciano"
        flavorText = "\"Remember, students, if it's glowing, humming, or whirring, don't touch it!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e811461a-6a90-416f-940c-c9579659ae0c.jpg?1783920127"
        ruling("2022-10-14", "If the target permanent has unearth and was put onto the battlefield using its unearth ability, this effect can still exile it and return it to its owner's hand.")
    }
}
