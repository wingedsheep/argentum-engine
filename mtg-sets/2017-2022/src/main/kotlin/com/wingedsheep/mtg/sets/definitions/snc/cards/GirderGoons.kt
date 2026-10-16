package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Girder Goons
 * {4}{B}
 * Creature — Ogre Warrior
 * 4/4
 * When this creature dies, create a tapped 2/2 black Rogue creature token.
 * Blitz {3}{B} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val GirderGoons = card("Girder Goons") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "When this creature dies, create a tapped 2/2 black Rogue creature token.\nBlitz {3}{B} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Rogue"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/0/6/06e503b9-a822-4c42-b478-1f598bc5941d.jpg?1783922961"
        )
    }

    keywordAbility(KeywordAbility.blitz("{3}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bfd6449-31a1-4572-8f9c-3f6f4670dc9d.jpg?1783923130"
    }
}
