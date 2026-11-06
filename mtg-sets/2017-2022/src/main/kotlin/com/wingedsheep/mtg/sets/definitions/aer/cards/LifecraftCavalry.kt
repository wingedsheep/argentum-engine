package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Lifecraft Cavalry
 * {4}{G}
 * Creature — Elf Warrior
 * 4/4
 *
 * Trample
 * Revolt — This creature enters with two +1/+1 counters on it if a permanent left the
 * battlefield under your control this turn.
 *
 * Revolt here is the condition of an "enters with counters" replacement effect, checked as the
 * creature enters (same shape as Greenwheel Liberator).
 *
 * Aether Revolt is the earliest printing.
 */
val LifecraftCavalry = card("Lifecraft Cavalry") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    oracleText = "Trample\nRevolt — This creature enters with two +1/+1 counters on it if a permanent left the battlefield under your control this turn."
    power = 4
    toughness = 4

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 2,
            selfOnly = true,
            condition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Svetlin Velinov"
        flavorText = "\"I do not ask you to be tame. I only ask you to be true.\"\n—Rishkar, Peema renegade"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2a92a03-4e1d-4b79-ad75-a29db2ea5495.jpg?1783936743"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
    }
}
