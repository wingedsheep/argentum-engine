package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Dragonlord's Prerogative — Dragons of Tarkir #52 (canonical printing)
 * {4}{U}{U} · Instant
 *
 * As an additional cost to cast this spell, you may reveal a Dragon card from your hand.
 * If you revealed a Dragon card or controlled a Dragon as you cast this spell, this spell can't be
 * countered.
 * Draw four cards.
 *
 * The reveal is the unnamed optional additional cost (`AdditionalCostWasPaid`); "controlled a Dragon
 * as you cast this spell" is a `captureAtCast`, frozen as the spell finishes being cast. Both are read
 * by `cantBeCounteredIf` off the spell on the stack whenever something tries to counter it. A copy
 * wasn't cast, so it keeps the protection only through a copied reveal (per the rulings).
 */
val DragonlordsPrerogative = card("Dragonlord's Prerogative") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may reveal a Dragon card from your hand.\n" +
        "If you revealed a Dragon card or controlled a Dragon as you cast this spell, this spell can't be countered.\n" +
        "Draw four cards."
    keywordAbility(
        KeywordAbility.optionalAdditionalCost(
            Costs.additional.RevealFromHand(GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
        )
    )
    cantBeCounteredIf = Conditions.Any(
        Conditions.AdditionalCostWasPaid,
        Conditions.CapturedAtCast("controlledDragon"),
    )
    spell {
        captureAtCast("controlledDragon", Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON)))
        effect = Effects.DrawCards(4)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53f2d2b3-6e48-413e-ad8f-9ce40c7ff167.jpg?1783938609"
        ruling("2015-02-25", "If one of these spells is copied, the controller of the copy will get the “Dragon bonus” only if a Dragon card was revealed as an additional cost. The copy wasn’t cast, so whether you controlled a Dragon won’t matter.")
        ruling("2015-02-25", "You can’t reveal more than one Dragon card to multiply the bonus. There is also no additional benefit for both revealing a Dragon card as an additional cost and controlling a Dragon as you cast the spell.")
        ruling("2015-02-25", "If you don’t reveal a Dragon card from your hand, you must control a Dragon as you are finished casting the spell to get the bonus. For example, if you lose control of your only Dragon while casting the spell (because, for example, you sacrificed it to activate a mana ability), you won’t get the bonus.")
    }
}
