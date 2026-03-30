package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Roughshod Duo
 * {2}{R}
 * Creature — Mouse Raccoon
 * 3/2
 *
 * Trample
 * Whenever you expend 4, target creature you control gets +1/+1 and gains
 * trample until end of turn.
 */
val RoughshodDuo = card("Roughshod Duo") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Mouse Raccoon"
    oracleText = "Trample\nWhenever you expend 4, target creature you control gets +1/+1 and " +
        "gains trample until end of turn. (You expend 4 as you spend your fourth total mana to " +
        "cast spells during a turn.)"
    power = 3
    toughness = 2

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.Expend(4)
        val t = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(1, 1, t)
            .then(Effects.GrantKeyword(Keyword.TRAMPLE, t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Michal Ivan"
        flavorText = "\"En garde, you inadequate excuse for a foe!\" challenged the mouse. \"Raaahrgh grrrr raaaargh!\" agreed the raccoon."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78cdcfb9-a247-4c2d-a098-5b57570f8cd5.jpg?1771346343"
    }
}
