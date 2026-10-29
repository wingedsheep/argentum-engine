package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Burlfist Oak
 * {2}{G}{G}
 * Creature — Treefolk
 * 2/3
 *
 * Whenever you draw a card, this creature gets +2/+2 until end of turn.
 *
 * `Triggers.you.draws()` fires once per card drawn, so drawing two cards gives +4/+4.
 */
val BurlfistOak = card("Burlfist Oak") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 2
    toughness = 3
    oracleText = "Whenever you draw a card, this creature gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Whenever you draw a card, this creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Milivoj Ćeran"
        flavorText = "If a tree fells you in the forest and no one is around to hear it, \"does it make a " +
            "sound\" is the last thing you need to worry about."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d0ec3bd-d894-4861-abcb-7b2e4f4de05c.jpg?1783930678"
        ruling(
            "2020-06-23",
            "If a spell or ability causes a player to put a card into their hand without specifically " +
                "using the word \"draw,\" it's not a card drawn."
        )
    }
}
