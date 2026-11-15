package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silumgar's Scorn — Dragons of Tarkir #78 (canonical printing)
 * {U}{U} · Instant
 *
 * As an additional cost to cast this spell, you may reveal a Dragon card from your hand.
 * Counter target spell unless its controller pays {1}. If you revealed a Dragon card or controlled a
 * Dragon as you cast this spell, counter that spell instead.
 *
 * The reveal is the unnamed optional additional cost (`AdditionalCostWasPaid`); "controlled a Dragon
 * as you cast this spell" is a `captureAtCast`, frozen as the spell finishes being cast. A copy wasn't
 * cast, so it keeps the hard counter only through a copied reveal (per the rulings).
 */
val SilumgarsScorn = card("Silumgar's Scorn") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may reveal a Dragon card from your hand.\n" +
        "Counter target spell unless its controller pays {1}. If you revealed a Dragon card or controlled a " +
        "Dragon as you cast this spell, counter that spell instead."
    keywordAbility(
        KeywordAbility.optionalAdditionalCost(
            Costs.additional.RevealFromHand(GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
        )
    )
    spell {
        captureAtCast("controlledDragon", Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON)))
        target(TargetFilter.SpellOnStack)
        effect = Effects.If(
            condition = Conditions.Any(
                Conditions.AdditionalCostWasPaid,
                Conditions.CapturedAtCast("controlledDragon"),
            ),
            then = Effects.CounterSpell(),
            otherwise = Effects.CounterUnlessPays("{1}"),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/077bee72-62f6-4d90-8557-ff9cac42ec9a.jpg?1783938602"
        ruling("2015-02-25", "If one of these spells is copied, the controller of the copy will get the “Dragon bonus” only if a Dragon card was revealed as an additional cost. The copy wasn’t cast, so whether you controlled a Dragon won’t matter.")
        ruling("2015-02-25", "You can’t reveal more than one Dragon card to multiply the bonus. There is also no additional benefit for both revealing a Dragon card as an additional cost and controlling a Dragon as you cast the spell.")
        ruling("2015-02-25", "If you don’t reveal a Dragon card from your hand, you must control a Dragon as you are finished casting the spell to get the bonus. For example, if you lose control of your only Dragon while casting the spell (because, for example, you sacrificed it to activate a mana ability), you won’t get the bonus.")
    }
}
