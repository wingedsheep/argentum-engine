package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Debtors' Transport — Ravnica Allegiance #72
 * {5}{B} · Creature — Thrull · 5/3
 *
 * Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature tokens with flying.)
 *
 * Afterlife is composed as its reminder text — a self-dies trigger creating the Spirit tokens — the
 * same shape as Ministrant of Obligation.
 */
val DebtorsTransport = card("Debtors' Transport") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    power = 5
    toughness = 3
    oracleText = "Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature " +
        "tokens with flying.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
        )
        description = "Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit " +
            "creature tokens with flying.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Jakub Kasper"
        flavorText = "By design, the sarcophagus muffles the debtors' moans but does not silence them."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cd03b99-d62e-428a-9e0d-097f1227a4da.jpg?1783933694"
        ruling(
            "2019-01-25",
            "Because blockers are chosen all at once, you can't block with a creature with afterlife, wait for " +
                "it to die, then block with the resulting Spirit tokens."
        )
    }
}
