package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mirror Image — Core Set 2019 #61
 * {2}{U} · Creature — Shapeshifter · Uncommon
 * 0/0
 *
 * You may have this creature enter as a copy of a creature you control.
 *
 * The standard [EntersAsCopy] clone replacement, restricted to a creature *you control*.
 * Declining leaves a 0/0 that state-based actions put into the graveyard.
 */
val MirrorImage = card("Mirror Image") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of a creature you control."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature.youControl()
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Randy Vargas"
        flavorText = "The life of a shapeshifter is one of constant change."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b3ffc69-f21b-410e-8993-8c1b4669fc19.jpg?1783934585"
        ruling(
            "2018-07-13",
            "If the chosen creature is copying something else (for example, if the chosen creature is " +
                "another Mirror Image), then Mirror Image enters the battlefield as whatever the chosen creature copied."
        )
        ruling(
            "2018-07-13",
            "If Mirror Image somehow enters the battlefield at the same time as another creature, it can't " +
                "become a copy of that creature. You may choose only a creature that's already on the battlefield."
        )
    }
}
