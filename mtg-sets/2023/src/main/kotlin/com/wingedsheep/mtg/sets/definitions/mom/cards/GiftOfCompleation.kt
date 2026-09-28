package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gift of Compleation — March of the Machine #106.
 * {1}{B} · Enchantment
 *
 * When this enchantment enters, incubate 3.
 * Whenever a Phyrexian you control dies, surveil 1.
 */
val GiftOfCompleation = card("Gift of Compleation") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, incubate 3. (Create an Incubator token with three +1/+1 counters on it and \"{2}: Transform this token.\" " +
        "It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "Whenever a Phyrexian you control dies, surveil 1. (Look at the top card of your library. You may put that card into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(3)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl().withSubtype("Phyrexian")).dies()
        effect = Patterns.Library.surveil(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97e08244-bbdb-402a-8dde-93e17d989467.jpg?1783917010"
    }
}
