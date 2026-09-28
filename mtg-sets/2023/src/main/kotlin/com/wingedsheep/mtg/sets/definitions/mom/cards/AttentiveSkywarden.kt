package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Attentive Skywarden — March of the Machine #7
 * {2}{W} · Creature — Phyrexian Kor · 2/2
 *
 * Flying
 * Whenever this creature deals combat damage to a player or battle, transform up to one target
 * Incubator token you control.
 *
 * "Incubator token" is the front face only: once transformed, the token is a Phyrexian artifact
 * creature and no longer has the Incubator subtype.
 */
val AttentiveSkywarden = card("Attentive Skywarden") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Kor"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player or battle, transform up to one target " +
        "Incubator token you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        val incubator = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype("Incubator").token().youControl()),
            optional = true
        )
        effect = Effects.Transform(incubator)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Jodie Muir"
        flavorText = "The same sturdy kitesail that once carried her safely through the Roil now allows " +
            "her to monitor the pods' progress from above."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83d89bd5-95cc-41fc-aea2-2dde52231919.jpg?1783917067"
    }
}
