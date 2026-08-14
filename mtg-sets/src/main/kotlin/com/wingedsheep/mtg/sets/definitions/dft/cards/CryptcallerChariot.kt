package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cryptcaller Chariot — Aetherdrift #80
 * {3}{B} · Artifact — Vehicle · 5/5
 *
 * Menace
 * Whenever you discard one or more cards, create that many tapped 2/2 black Zombie creature tokens.
 * Crew 2
 *
 * Batch-worded discard payoff (CR 603.2c), so it uses [Triggers.YouDiscardOneOrMore] — one trigger
 * per discard *event* however many cards it held — and reads the batch size back through
 * [ContextPropertyKey.TRIGGER_DISCARD_COUNT] for "that many". Discarding three cards to a single
 * effect makes three Zombies; three separate discards make one each. Same shape as its set-mates
 * Marauding Mako and Magmakin Artillerist.
 *
 * The Zombies enter tapped, so they can't crew the Chariot the turn they arrive — the card wants
 * you to crew first and discard after.
 */
val CryptcallerChariot = card("Cryptcaller Chariot") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Menace\n" +
        "Whenever you discard one or more cards, create that many tapped 2/2 black Zombie " +
        "creature tokens.\n" +
        "Crew 2"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.YouDiscardOneOrMore
        effect = Effects.CreateToken(
            count = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DISCARD_COUNT),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/b/8/b82be730-c63b-4c2b-99f4-476befdb95cb.jpg?1783907681",
        )
        description = "Whenever you discard one or more cards, create that many tapped 2/2 black " +
            "Zombie creature tokens."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Aaron Miller"
        flavorText = "\"They too deserve to witness Amonkhet's rebirth.\"\n—Zahur"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0c8259c-055e-4bff-b945-c0ecb057a8f0.jpg?1783907897"
    }
}
