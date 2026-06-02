package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
/**
 * Reya Dawnbringer
 * {6}{W}{W}{W}
 * Legendary Creature — Angel
 * 4/6
 * Flying
 * At the beginning of your upkeep, you may return target creature card from your graveyard to the battlefield.
 */
val ReyaDawnbringer = card("Reya Dawnbringer") {
    manaCost = "{6}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Angel"
    power = 4
    toughness = 6
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, you may return target creature card from your graveyard to the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        val t = target("target creature card from your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = MayEffect(Effects.Move(t, Zone.BATTLEFIELD))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Matthew D. Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1e0e72b-e65e-4578-b610-9f529daa32d7.jpg?1562940371"
    }
}
