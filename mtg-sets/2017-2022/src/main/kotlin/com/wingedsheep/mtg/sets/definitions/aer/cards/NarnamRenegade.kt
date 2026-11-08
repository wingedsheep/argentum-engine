package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Narnam Renegade
 * {G}
 * Creature — Elf Warrior
 * 1/2
 *
 * Deathtouch
 * Revolt — This creature enters with a +1/+1 counter on it if a permanent left the
 * battlefield under your control this turn.
 *
 * Revolt here is the condition of an enters-with-counters replacement effect, checked against
 * the per-player [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker (same shape as
 * Greenwheel Liberator).
 *
 * Aether Revolt is the earliest printing.
 */
val NarnamRenegade = card("Narnam Renegade") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    oracleText = "Deathtouch\nRevolt — This creature enters with a +1/+1 counter on it if a permanent left the battlefield under your control this turn."
    power = 1
    toughness = 2

    keywords(Keyword.DEATHTOUCH)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            selfOnly = true,
            condition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Greg Opalinski"
        flavorText = "\"I do not need to pierce your heart to still its beating.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/4/041cc6b1-61c8-4c34-86fc-d647ce306f59.jpg?1783936742"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
    }
}
