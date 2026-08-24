package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Angry Mob
 * {2}{W}{W}
 * Creature — Human
 * Power and toughness are each "2+&#42;" on the printed card.
 * Trample
 * During your turn, Angry Mob's power and toughness are each equal to 2 plus the number of Swamps
 * your opponents control. During turns other than yours, Angry Mob's power and toughness are each 2.
 *
 * A characteristic-defining ability, so it goes through `dynamicStats` — a base-setting layer 7b
 * effect rather than a pump, which is what makes a later "becomes a 0/2" overwrite it instead of
 * stacking on top of it.
 *
 * The turn clause folds into the amount rather than into a second ability. Both printed sentences
 * describe the *same* CDA reading a different value, and one `Conditional` amount keeps them from
 * drifting into two effects that could disagree — off your turn the whole "plus Swamps" term simply
 * isn't there.
 *
 * The constant 2 is the printed *offset*, so it rides in `powerOffset`/`toughnessOffset` rather than
 * inside the amount. Both spellings compute the same numbers, but only the offset form renders as
 * `*+2` (`CharacteristicValue.DynamicWithOffset`, same shape as Tarmogoyf); folding it into the
 * amount produces a plain `CharacteristicValue.Dynamic`, whose description is a bare "*".
 *
 * "Swamps your opponents control" is the land subtype, so a nonbasic land with the Swamp type
 * counts, and a Swamp *you* control never does.
 */
val AngryMob = card("Angry Mob") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    oracleText = "Trample\n" +
        "During your turn, Angry Mob's power and toughness are each equal to 2 plus the number of " +
        "Swamps your opponents control. During turns other than yours, Angry Mob's power and " +
        "toughness are each 2."

    keywords(Keyword.TRAMPLE)

    dynamicStats(
        DynamicAmount.Conditional(
            condition = Conditions.IsYourTurn,
            ifTrue = DynamicAmounts
                .battlefield(Player.EachOpponent, GameObjectFilter.Land.withSubtype(Subtype.SWAMP))
                .count(),
            ifFalse = DynamicAmount.Fixed(0),
        ),
        powerOffset = 2,
        toughnessOffset = 2,
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e14db1c-0a05-47d2-9f27-df881f7f37ab.jpg?1783947950"
    }
}
