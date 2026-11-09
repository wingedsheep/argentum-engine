package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angel of Deliverance (Shadows over Innistrad #2)
 * {6}{W}{W}
 * Creature — Angel
 * 6/6
 *
 * Flying
 * Delirium — Whenever this creature deals damage, if there are four or more card types among cards
 * in your graveyard, exile target creature an opponent controls.
 */
val AngelOfDeliverance = card("Angel of Deliverance") {
    manaCost = "{6}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Delirium — Whenever this creature deals damage, if there are four or more card types among " +
        "cards in your graveyard, exile target creature an opponent controls."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsDamage()
        interveningIf = Conditions.Delirium()
        val t = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4e12f83-eab8-4113-ab63-3f7a830861d4.jpg"
    }
}
