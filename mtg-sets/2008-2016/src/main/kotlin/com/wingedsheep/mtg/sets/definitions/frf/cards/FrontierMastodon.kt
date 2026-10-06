package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Frontier Mastodon
 * {2}{G}
 * Creature — Elephant
 * 3/2
 *
 * Ferocious — This creature enters with a +1/+1 counter on it if you control a creature with
 * power 4 or greater.
 *
 * Per the 2014-11-24 ruling the check happens as the Mastodon enters, while it isn't on the
 * battlefield yet, so it never counts itself — hence `excludeSelf`.
 */
val FrontierMastodon = card("Frontier Mastodon") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant"
    power = 3
    toughness = 2
    oracleText = "Ferocious — This creature enters with a +1/+1 counter on it if you control a creature with power 4 or greater."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Exists(
            Player.You,
            Zone.BATTLEFIELD,
            GameObjectFilter.Creature.powerAtLeast(4),
            excludeSelf = true
        )
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Nils Hamm"
        flavorText = "Each bronze disk on its harness is an offering from a young Temur hunter, a sign of respect to the spirits of the wild."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46c09fe7-d55f-49b4-95c9-a3bb36baa3aa.jpg?1783938681"
        ruling(
            "2014-11-24",
            "Frontier Mastodon's ferocious ability checks if you control a creature with power 4 or greater as Frontier Mastodon enters the battlefield. Because Frontier Mastodon isn't on the battlefield at this time, it won't count itself."
        )
    }
}
