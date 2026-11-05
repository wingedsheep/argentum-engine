package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sifter Wurm
 * {5}{G}{G}
 * Creature — Wurm
 * 7/7
 * Trample
 * When this creature enters, scry 3, then reveal the top card of your library. You gain life
 * equal to that card's mana value.
 *
 * Interpret the Signs' shape with life gain instead of draws: the revealed card stays on top of
 * the library (revealing doesn't move it); its mana value is read from the gathered collection.
 */
val SifterWurm = card("Sifter Wurm") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 7
    toughness = 7
    oracleText = "Trample\nWhen this creature enters, scry 3, then reveal the top card of your library. " +
        "You gain life equal to that card's mana value."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(3) then Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, Player.You))
            reveal(top)
            run(Effects.GainLife(DynamicAmounts.manaValueOf(top)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Filip Burburan"
        flavorText = "\"Rhonas's monument belongs to the wurms now. Look elsewhere for refuge.\"\n—Arnun, vizier of the menagerie"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dbaf7e3-e2fc-4399-aa83-c13df43008a0.jpg?1783936012"
        ruling(
            "2020-11-10",
            "Once Sifter Wurm's triggered ability begins to resolve, no player may take other actions until " +
                "it's done. Notably, opponents can't try to change your library after you scry but before you " +
                "reveal the top card of your library."
        )
        ruling("2020-11-10", "For cards in your library with {X} in their mana costs, X is considered to be 0.")
    }
}
