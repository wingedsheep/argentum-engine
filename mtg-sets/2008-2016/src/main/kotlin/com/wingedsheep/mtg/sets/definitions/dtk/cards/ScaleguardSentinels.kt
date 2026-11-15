package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Scaleguard Sentinels — Dragons of Tarkir #201 (canonical printing)
 * {G}{G} · Creature — Human Soldier · 2/3
 *
 * As an additional cost to cast this spell, you may reveal a Dragon card from your hand.
 * This creature enters with a +1/+1 counter on it if you revealed a Dragon card or controlled a
 * Dragon as you cast this spell.
 *
 * Orator of Ojutai's shape with an enters-with replacement in place of the enters trigger: the
 * reveal is the optional additional cost, the controlled Dragon a `spell { captureAtCast }` the
 * permanent reads as it enters.
 */
val ScaleguardSentinels = card("Scaleguard Sentinels") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "As an additional cost to cast this spell, you may reveal a Dragon card from your hand.\n" +
        "This creature enters with a +1/+1 counter on it if you revealed a Dragon card or controlled " +
        "a Dragon as you cast this spell."

    keywordAbility(
        KeywordAbility.optionalAdditionalCost(
            Costs.additional.RevealFromHand(GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
        )
    )
    spell {
        captureAtCast("controlledDragon", Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON)))
    }

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true,
            condition = Conditions.Any(
                Conditions.AdditionalCostWasPaid,
                Conditions.CapturedAtCast("controlledDragon"),
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/701da01c-0f27-4cb2-8bfa-407239778982.jpg?1783938577"
        ruling("2015-02-25", "If one of these spells is copied, the controller of the copy will get the \"Dragon bonus\" only if a Dragon card was revealed as an additional cost. The copy wasn't cast, so whether you controlled a Dragon won't matter.")
        ruling("2015-02-25", "You can't reveal more than one Dragon card to multiply the bonus. There is also no additional benefit for both revealing a Dragon card as an additional cost and controlling a Dragon as you cast the spell.")
        ruling("2015-02-25", "If you don't reveal a Dragon card from your hand, you must control a Dragon as you are finished casting the spell to get the bonus. For example, if you lose control of your only Dragon while casting the spell (because, for example, you sacrificed it to activate a mana ability), you won't get the bonus.")
    }
}
