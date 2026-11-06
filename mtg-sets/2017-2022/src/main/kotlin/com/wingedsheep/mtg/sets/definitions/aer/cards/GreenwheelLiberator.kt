package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Greenwheel Liberator
 * {1}{G}
 * Creature — Elf Warrior
 * 2/1
 *
 * Revolt — This creature enters with two +1/+1 counters on it if a permanent left the
 * battlefield under your control this turn.
 *
 * "Enters with counters" is a replacement effect (CR 614.1c), not a trigger, so revolt is the
 * replacement's condition, checked as the creature enters against the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker.
 *
 * Aether Revolt is the earliest printing.
 */
val GreenwheelLiberator = card("Greenwheel Liberator") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    oracleText = "Revolt — This creature enters with two +1/+1 counters on it if a permanent left the battlefield under your control this turn."
    power = 2
    toughness = 1

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 2,
            selfOnly = true,
            condition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Todd Lockwood"
        flavorText = "\"I will not rest until every vestige of the Consulate—every banner, flag, and standard—has fallen.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ad9bf0f-4271-497f-8d67-3c7bf09342dc.jpg?1783936744"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
    }
}
