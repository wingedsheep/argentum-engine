package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Orator of Ojutai — Dragons of Tarkir #28 (canonical printing)
 * {1}{W} · Creature — Bird Monk · 0/4
 *
 * As an additional cost to cast this spell, you may reveal a Dragon card from your hand.
 * Defender, flying
 * When this creature enters, if you revealed a Dragon card or controlled a Dragon as you cast this
 * spell, draw a card.
 *
 * The reveal is the unnamed optional additional cost (read back by `AdditionalCostWasPaid`);
 * "controlled a Dragon as you cast this spell" is a `spell { captureAtCast }`, frozen as the spell
 * finishes being cast and carried onto the permanent (and into its enters trigger), so a Dragon lost before the trigger resolves
 * still counts and one lost mid-cast (sacrificed for mana) doesn't (per the rulings).
 */
val OratorOfOjutai = card("Orator of Ojutai") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Monk"
    power = 0
    toughness = 4
    oracleText = "As an additional cost to cast this spell, you may reveal a Dragon card from your hand.\n" +
        "Defender, flying\n" +
        "When this creature enters, if you revealed a Dragon card or controlled a Dragon as you cast " +
        "this spell, draw a card."

    keywordAbility(
        KeywordAbility.optionalAdditionalCost(
            Costs.additional.RevealFromHand(GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
        )
    )
    spell {
        captureAtCast("controlledDragon", Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON)))
    }

    keywords(Keyword.DEFENDER, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Any(
            Conditions.AdditionalCostWasPaid,
            Conditions.CapturedAtCast("controlledDragon"),
        )
        effect = Effects.DrawCards(1)
        description = "When this creature enters, if you revealed a Dragon card or controlled a Dragon " +
            "as you cast this spell, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc68c7d8-60fa-4e55-92c1-43fcf5e9abf1.jpg?1783938616"
        ruling("2015-02-25", "If one of these spells is copied, the controller of the copy will get the “Dragon bonus” only if a Dragon card was revealed as an additional cost. The copy wasn’t cast, so whether you controlled a Dragon won’t matter.")
        ruling("2015-02-25", "You can’t reveal more than one Dragon card to multiply the bonus. There is also no additional benefit for both revealing a Dragon card as an additional cost and controlling a Dragon as you cast the spell.")
        ruling("2015-02-25", "If you don’t reveal a Dragon card from your hand, you must control a Dragon as you are finished casting the spell to get the bonus. For example, if you lose control of your only Dragon while casting the spell (because, for example, you sacrificed it to activate a mana ability), you won’t get the bonus.")
    }
}
