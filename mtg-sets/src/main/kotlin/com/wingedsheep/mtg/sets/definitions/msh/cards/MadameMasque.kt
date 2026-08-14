package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Madame Masque
 * {4}{B}
 * Legendary Creature — Human Villain
 * 3/2
 *
 * When Madame Masque enters, she connives.
 * Whenever you draw your second card each turn, create a 2/1 black Villain creature token
 * with menace.
 *
 * "She connives" is the source conniving, i.e. the plain [Effects.Connive] on `EffectTarget.Self`
 * (draw, discard, and a +1/+1 counter if the discard was a nonland card) — the same shape
 * A.I.M. Scientists uses in this set.
 *
 * The draw payoff is [Triggers.NthCardDrawn]`(2)` (CR 121.2): it reads the per-turn draw counter,
 * so it fires exactly once per turn on the crossing into the second draw — a single two-card draw
 * fires it once, not twice. Note the connive's own draw counts toward that tally. The token is the
 * unnamed 2/1 black Villain with menace this set already mints elsewhere.
 */
val MadameMasque = card("Madame Masque") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Villain"
    power = 3
    toughness = 2
    oracleText = "When Madame Masque enters, she connives. (Draw a card, then discard a card. " +
        "If you discarded a nonland card, put a +1/+1 counter on this creature.)\n" +
        "Whenever you draw your second card each turn, create a 2/1 black Villain creature token " +
        "with menace. (It can't be blocked except by two or more creatures.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Connive()
        description = "When Madame Masque enters, she connives."
    }

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf(Subtype.VILLAIN.value),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804",
        )
        description = "Whenever you draw your second card each turn, create a 2/1 black Villain " +
            "creature token with menace."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f54197b8-6279-43ee-9340-69bd22cf3775.jpg?1783902943"
    }
}
