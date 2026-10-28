package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.model.Rarity

/**
 * Rutherford, Upgraded Ensign
 * {2}{W}
 * Legendary Creature — Human Artificer
 * 2/2
 *
 * When Rutherford enters, look at the top six cards of your library. You may reveal an artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order.
 * Whenever you cast a Spacecraft spell, put a +1/+1 counter on target creature.
 */
val RutherfordUpgradedEnsign = card("Rutherford, Upgraded Ensign") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Artificer"
    colorIdentity = "W"
    oracleText = "When Rutherford enters, look at the top six cards of your library. You may reveal an artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order.\nWhenever you cast a Spacecraft spell, put a +1/+1 counter on target creature."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 6,
            filter = GameObjectFilter.Artifact,
            prompt = "You may reveal an artifact card from among them and put it into your hand"
        )
    }
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype("Spacecraft"))
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Charles Chaisson"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16a4d8ed-a918-4dfe-9976-0b68adb26afa.jpg?1792753861"
    }
}
