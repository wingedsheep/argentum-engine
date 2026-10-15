package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wingshield Agent
 * {2}{U}
 * Creature — Human Soldier
 * 2 / 3
 * This creature enters with a shield counter on it.
 * Whenever this creature attacks, up to one other target creature gains flying until end of turn.
 */
val WingshieldAgent = card("Wingshield Agent") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    oracleText = "This creature enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)\n" +
        "Whenever this creature attacks, up to one other target creature gains flying until end of turn."
    power = 2
    toughness = 3

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 1,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Aaron J. Riley"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9ec36ec-1a9c-41db-b304-c23286a4d182.jpg"
    }
}
