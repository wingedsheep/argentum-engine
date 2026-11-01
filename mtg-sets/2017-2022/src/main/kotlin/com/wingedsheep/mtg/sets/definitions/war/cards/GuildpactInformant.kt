package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Guildpact Informant
 * {2}{U}
 * Creature — Faerie Rogue
 * 1/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player or planeswalker, proliferate.
 */
val GuildpactInformant = card("Guildpact Informant") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Faerie Rogue"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player or planeswalker, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "271"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a304200f-86d5-4eed-a775-cc24e72872ac.jpg?1783933359"
    }
}
