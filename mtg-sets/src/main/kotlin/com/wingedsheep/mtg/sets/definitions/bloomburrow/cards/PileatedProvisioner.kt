package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pileated Provisioner
 * {4}{W}
 * Creature — Bird Scout
 * 3/4
 *
 * Flying
 * When this creature enters, put a +1/+1 counter on target creature
 * you control without flying.
 */
val PileatedProvisioner = card("Pileated Provisioner") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Bird Scout"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, put a +1/+1 counter on target creature you control without flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("creature without flying", TargetObject(
            filter = TargetFilter.CreatureYouControl.withoutKeyword(Keyword.FLYING)
        ))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Eelis Kyttanen"
        flavorText = "Aerial supporters train for years to make sure the weapons they drop land in the hands of mice and not on their heads."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae442cd6-c4df-4aad-9b1d-ccd936c5ec96.jpg?1721425901"
    }
}
