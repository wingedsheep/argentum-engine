package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shipwreck Sifters (Innistrad: Midnight Hunt #74)
 * {1}{U} · Creature — Spirit 1/2
 * When this creature enters, draw a card, then discard a card.
 * Whenever you discard a Spirit card or a card with disturb, put a +1/+1 counter on this creature.
 *
 * The discard trigger fires once per matching card. "A card with disturb" reads the DISTURB
 * keyword the card builder derives from each card's `disturb(...)` keyword ability.
 */
val ShipwreckSifters = card("Shipwreck Sifters") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, draw a card, then discard a card.\n" +
        "Whenever you discard a Spirit card or a card with disturb, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot()
    }

    triggeredAbility {
        trigger = Triggers.you.discards(
            GameObjectFilter.Any.withSubtype(Subtype.SPIRIT)
                .or(GameObjectFilter.Any.withKeyword(Keyword.DISTURB))
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Svetlin Velinov"
        flavorText = "A loyal crew sticks together, even after death."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f295833-a83e-4b18-8007-c7f5d78e5624.jpg?1783925631"
    }
}
