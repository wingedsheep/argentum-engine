package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yoshimaru, Ever Faithful
 * {W}
 * Legendary Creature — Dog
 * 1/1
 * Whenever another legendary permanent you control enters, put a +1/+1 counter on Yoshimaru.
 * Partner (You can have two commanders if both have partner.)
 *
 * Partner is a deck-construction ability (CR 702.124h) and does nothing during play.
 */
val YoshimaruEverFaithful = card("Yoshimaru, Ever Faithful") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dog"
    power = 1
    toughness = 1
    oracleText = "Whenever another legendary permanent you control enters, put a +1/+1 counter on Yoshimaru.\n" +
        "Partner (You can have two commanders if both have partner.)"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.legendary().youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    keywordAbility(KeywordAbility.partner())

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "32"
        artist = "Ilse Gort"
        flavorText = "Day after day he sat there, knowing that the Wanderer would soon be back for him."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa409269-3698-42a2-8c51-75557b27a6f6.jpg?1783923987"
    }
}
