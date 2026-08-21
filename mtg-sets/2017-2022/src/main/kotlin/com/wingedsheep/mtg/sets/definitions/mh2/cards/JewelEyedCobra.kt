package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jewel-Eyed Cobra — Modern Horizons 2 #168
 * {2}{G} · Creature — Snake · 3 / 1
 *
 * Deathtouch
 * When this creature dies, create a Treasure token. (It's an artifact with "{T}, Sacrifice this
 * token: Add one mana of any color.")
 *
 * [Triggers.Dies] is the battlefield → graveyard zone change bound to the source itself, so the
 * ability reads last-known information about the Cobra — nothing here needs the dead permanent's
 * characteristics, only the fact that it left. The Treasure is the predefined token
 * ([Effects.CreateTreasure]) rather than a hand-rolled artifact token, so it shares the corpus's
 * single Treasure definition and its art.
 */
val JewelEyedCobra = card("Jewel-Eyed Cobra") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 1
    oracleText = "Deathtouch\n" +
        "When this creature dies, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Josu Hernaiz"
        flavorText = "\"Sure, they're worth a lot, but I'd rather survive to spend my earnings.\"\n—Mokgar, Kalonian hunter"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f8db60d-3adf-45e1-b4d0-3b5e24f5e01d.jpg?1783926831"
    }
}
