package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Airdrop Aeronauts
 * {3}{W}{W}
 * Creature — Dwarf Scout
 * 4/3
 *
 * Flying
 * Revolt — When this creature enters, if a permanent left the battlefield under your control
 * this turn, you gain 5 life.
 *
 * Revolt is a CR 603.4 intervening-if, checked when the creature enters and again on
 * resolution, so it rides on `interveningIf` with the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker.
 *
 * Aether Revolt is the earliest printing; the only later one is Kaladesh Remastered (KLR),
 * which isn't scaffolded.
 */
val AirdropAeronauts = card("Airdrop Aeronauts") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Scout"
    oracleText = "Flying\nRevolt — When this creature enters, if a permanent left the battlefield under your control this turn, you gain 5 life."
    power = 4
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.GainLife(5)
        description = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, you gain 5 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Greg Opalinski"
        flavorText = "\"Supplies inbound. Keep that pressure on.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49827a57-cf10-4a44-a1fd-ac611da39dc9.jpg?1783936783"
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. Tokens that leave the battlefield will satisfy a revolt ability; paying {E} won't.")
    }
}
