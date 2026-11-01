package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Mowu, Loyal Companion — War of the Spark #167
 * {3}{G} · Legendary Creature — Dog · 3/3
 *
 * Vigilance, trample
 * If one or more +1/+1 counters would be put on Mowu, that many plus one +1/+1 counters are put
 * on it instead.
 *
 * The replacement is Hardened Scales' [ModifyCounterPlacement] with the recipient narrowed to
 * [Recipient.Self]. The placer stays `Player.Any` — the printed text applies whoever puts the
 * counters on Mowu, an opponent included.
 */
val MowuLoyalCompanion = card("Mowu, Loyal Companion") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Dog"
    power = 3
    toughness = 3
    oracleText = "Vigilance, trample\n" +
        "If one or more +1/+1 counters would be put on Mowu, that many plus one +1/+1 counters " +
        "are put on it instead."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                recipient = Recipient.Self,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Kimonas Theodossiou"
        flavorText = "Wherever Yanggu goes, Mowu follows—sometimes at his side, sometimes obscuring " +
            "him from view entirely."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f41a191c-c42e-42ec-89bd-cc1bc215ffbc.jpg?1783933409"
    }
}
