package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sandsteppe Mastodon — Fate Reforged #137 (canonical printing)
 * {5}{G}{G} · Creature — Elephant · 5/5
 *
 * Reach
 * When this creature enters, bolster 5.
 */
val SandsteppeMastodon = card("Sandsteppe Mastodon") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant"
    power = 5
    toughness = 5
    oracleText = "Reach\n" +
        "When this creature enters, bolster 5. (Choose a creature with the least toughness among " +
        "creatures you control and put five +1/+1 counters on it.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Mechanic.bolster(5)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "James Zapata"
        flavorText = "A mastodon's path always leads to an oasis."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76ae01fb-12fa-444d-9d49-bfe195c2a5a6.jpg?1783938678"
        ruling("2014-11-24", "Bolster itself doesn't target any creature, though some spells and abilities that bolster may have other effects that target creatures. For example, you could put counters on a creature with protection from white with Abzan Skycaptain's bolster ability.")
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
