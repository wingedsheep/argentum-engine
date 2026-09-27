package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Corruption of Towashi
 * {4}{U}
 * Enchantment
 *
 * When this enchantment enters, incubate 4. (Create an Incubator token with four +1/+1 counters on
 * it and "{2}: Transform this token." It transforms into a 0/0 Phyrexian artifact creature.)
 * Whenever a permanent you control transforms or a permanent you control enters transformed, you
 * may draw a card. Do this only once each turn.
 */
val CorruptionOfTowashi = card("Corruption of Towashi") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, incubate 4. (Create an Incubator token with four +1/+1 counters " +
        "on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "Whenever a permanent you control transforms or a permanent you control enters transformed, you may " +
        "draw a card. Do this only once each turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(4)
    }

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.a(GameObjectFilter.Permanent.youControl()).transforms(),
            // "Enters transformed" — enters back face up (CR 701.27g's transformed permanent).
            Triggers.a(GameObjectFilter.Permanent.youControl().transformed()).enters()
        )
        // "Do this only once each turn" is keyed to the draw, not the trigger: a declined draw
        // leaves the ability live for the next transform (ruling 2023-04-14).
        effectOncePerTurn = true
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e896a87-2a93-488c-a0c9-0bed8404da44.jpg?1783917039"
        ruling("2023-04-14", "Only a transforming double-faced permanent can enter the battlefield \"transformed,\" and only if it enters with its back face up. Notably, melded permanents can't enter transformed, and modal double-faced permanents can't enter transformed, even if they enter with their back faces up.")
        ruling("2023-04-14", "Similarly, only transforming double-faced permanents (including transforming double-faced cards and Incubator tokens) can transform. A face-up permanent turning face down doesn't count as transforming, nor does a face-down permanent turning face up.")
        ruling("2023-04-14", "The last ability of Corruption of Towashi will trigger if a permanent you control transforms in either direction, going from front face up to back face up or vice versa.")
        ruling("2023-04-14", "Once you've chosen to draw a card because of the last ability (which is likely as soon as possible, but there may be situations in which you don't want to draw immediately), that ability will stop triggering for the duration of that turn.")
    }
}
