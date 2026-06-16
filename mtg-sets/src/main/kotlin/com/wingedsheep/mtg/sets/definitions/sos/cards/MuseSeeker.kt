package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.opus
import com.wingedsheep.sdk.model.Rarity

/**
 * Muse Seeker
 * {1}{U}
 * Creature — Elf Wizard
 * 1/2
 *
 * Opus — Whenever you cast an instant or sorcery spell, draw a card. Then discard a card
 * unless five or more mana was spent to cast that spell.
 *
 * "Opus" is an ability word (flavor only). The `opus { }` builder wires the spell-cast trigger
 * and the 5+ mana tier. The discard is the *low* tier here: base = draw then discard, and when
 * five or more mana was spent the discard is dropped — so `insteadIfFiveOrMore` replaces the
 * base with a bare draw.
 */
val MuseSeeker = card("Muse Seeker") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elf Wizard"
    power = 1
    toughness = 2
    oracleText = "Opus — Whenever you cast an instant or sorcery spell, draw a card. Then discard " +
        "a card unless five or more mana was spent to cast that spell."

    opus {
        effect = Effects.DrawCards(1).then(Effects.Discard(1))
        insteadIfFiveOrMore = Effects.DrawCards(1)
        description = "Opus — Whenever you cast an instant or sorcery spell, draw a card. Then " +
            "discard a card unless five or more mana was spent to cast that spell."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Dan Murayama Scott"
        flavorText = "\"Failure can be a reason to quit or fuel for inspiration. The choice is " +
            "always yours.\"\n—Veyran, dean of perfection"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71cb4a6b-b500-4b28-bcdb-ec4188242f39.jpg?1775937328"
    }
}
