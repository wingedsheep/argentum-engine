package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cached Defenses
 * {2}{G}
 * Sorcery
 * Bolster 3. (Choose a creature with the least toughness among creatures you control and put
 * three +1/+1 counters on it.)
 *
 * The bolstered creature is chosen as the spell resolves; bolster doesn't target. With no
 * creatures, the spell resolves and does nothing.
 */
val CachedDefenses = card("Cached Defenses") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Bolster 3. " +
        "(Choose a creature with the least toughness among creatures you control and put three +1/+1 counters on it.)"

    spell {
        effect = Patterns.Mechanic.bolster(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Zack Stella"
        flavorText = "The glittering scales in the Abzan vaults represent mighty deeds of the past and protection for generations to come."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41ad36cf-5f29-41c4-b2a3-6b8e8c0c493f.jpg?1783938684"
        ruling("2014-11-24", "Bolster itself doesn't target any creature, though some spells and abilities that bolster may have other effects that target creatures. For example, you could put counters on a creature with protection from white with Abzan Skycaptain's bolster ability.")
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
