package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Garrison Griffin
 * {2}{W}
 * Creature — Griffin
 * 2/2
 *
 * Flying
 * Whenever this creature attacks, target Knight you control gains flying until end of turn.
 */
val GarrisonGriffin = card("Garrison Griffin") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    oracleText = "Flying\n" +
        "Whenever this creature attacks, target Knight you control gains flying until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Knight").youControl()))
        effect = Effects.GrantKeyword(Keyword.FLYING, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "305"
        artist = "Jason Kang"
        flavorText = "The keen-eyed guardians of Ardenvale are ever on alert."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94a04717-5cd7-4982-bf36-00cea30b9ef1.jpg?1783932556"
    }
}
