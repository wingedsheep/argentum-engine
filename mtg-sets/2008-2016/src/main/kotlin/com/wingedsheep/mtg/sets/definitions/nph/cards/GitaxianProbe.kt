package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gitaxian Probe
 * {U/P}
 * Sorcery
 *
 * ({U/P} can be paid with either {U} or 2 life.)
 * Look at target player's hand.
 * Draw a card.
 *
 * The Phyrexian pip is parsed directly by `ManaCost.parse` (same as Dismember); it always counts
 * 1 toward mana value regardless of how it's paid.
 */
val GitaxianProbe = card("Gitaxian Probe") {
    manaCost = "{U/P}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at target player's hand.\nDraw a card."
    spell {
        val t = target(Targets.Player)
        effect = Effects.LookAtHand(t) then Effects.DrawCards(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Chippy"
        flavorText = "\"My flesh holds no secrets, monster. The spirit of Mirrodin will fight on.\"\n" +
            "—Vy Covalt, Mirran resistance"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/995486ce-58bb-4753-a812-0ca73ef1a235.jpg?1783941320"
        ruling(
            "2011-06-01",
            "To calculate the mana value of a card with Phyrexian mana symbols in its cost, count " +
                "each Phyrexian mana symbol as 1."
        )
        ruling("2011-06-01", "If you're at 1 life or less, you can't pay 2 life.")
        ruling("2011-06-01", "The targeted player may have no cards in their hand. You'll still draw a card.")
    }
}
