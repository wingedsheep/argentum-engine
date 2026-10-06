package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower

/**
 * Howlgeist — Avacyn Restored #182.
 * {5}{G} Creature — Spirit Wolf 4/2
 *
 * "Creatures with power less than this creature's power can't block it." is the attacker-side static
 * [CantBeBlockedByCreaturesWithLessPower] (Shrill Howler, Formation Breaker), compared on projected
 * power when blockers are declared. Undying returns it as a 5/3, raising the threshold accordingly.
 */
val Howlgeist = card("Howlgeist") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit Wolf"
    oracleText = "Creatures with power less than this creature's power can't block it.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the " +
        "battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 4
    toughness = 2
    keywords(Keyword.UNDYING)

    staticAbility {
        ability = CantBeBlockedByCreaturesWithLessPower()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dad60d45-1c99-41d1-a237-c0ee18ce5361.jpg?1783940666"
        ruling(
            "2012-05-01",
            "The comparison of power is performed only when blockers are declared. Increasing the " +
                "power of a blocking creature (or decreasing the power of Howlgeist) after this point " +
                "won't cause any creature to stop blocking or become unblocked."
        )
    }
}
