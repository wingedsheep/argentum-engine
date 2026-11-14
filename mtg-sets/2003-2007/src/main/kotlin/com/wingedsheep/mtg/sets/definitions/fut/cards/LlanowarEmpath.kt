package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Llanowar Empath
 * {3}{G}
 * Creature — Elf Shaman
 * 2/2
 * When this creature enters, scry 2, then reveal the top card of your library. If it's a creature
 * card, put it into your hand.
 *
 * Sifter Wurm's scry-then-reveal shape with Doors of Durin's creature filter: only a creature card
 * moves to hand; a revealed noncreature card stays on top of the library (revealing doesn't move it).
 */
val LlanowarEmpath = card("Llanowar Empath") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Shaman"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, scry 2, then reveal the top card of your library. " +
        "If it's a creature card, put it into your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2) then Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, Player.You))
            reveal(top)
            val topCreature = filter(top, GameObjectFilter.Creature)
            toHand(topCreature)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf831ed3-d3ae-43ac-82ca-4984c05dde86.jpg?1783943099"
    }
}
