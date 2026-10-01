package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Bloated Contaminator
 * {2}{G}
 * Creature — Phyrexian Beast
 * 4/4
 *
 * Trample
 * Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)
 * Whenever this creature deals combat damage to a player, proliferate.
 */
val BloatedContaminator = card("Bloated Contaminator") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Phyrexian Beast"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "Whenever this creature deals combat damage to a player, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/411bda96-6c65-475d-9850-0a9b3eefa553.jpg?1783918018"
    }
}
