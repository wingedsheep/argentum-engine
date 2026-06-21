package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bashful Beastie
 * {4}{G}
 * Creature — Beast
 * 5/4
 * When this creature dies, manifest dread. (Look at the top two cards of your library. Put one
 * onto the battlefield face down as a 2/2 creature and the other into your graveyard. Turn it face
 * up any time for its mana cost if it's a creature card.)
 *
 * Dies-trigger flavor of the shared [Patterns.Library.manifestDread] recipe (CR 701.62b).
 */
val BashfulBeastie = card("Bashful Beastie") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "When this creature dies, manifest dread. (Look at the top two cards of your " +
        "library. Put one onto the battlefield face down as a 2/2 creature and the other into your " +
        "graveyard. Turn it face up any time for its mana cost if it's a creature card.)"
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Patterns.Library.manifestDread()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Aaron Miller"
        flavorText = "No one knows what hides beneath the beasties' masks."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c20fa7ee-a4c2-4eb0-9467-195f3b894fa0.jpg?1726286489"
    }
}
