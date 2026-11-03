package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * In the Pale Moonlight — Star Trek #117
 * {3}{B}{B} · Enchantment — Saga
 *
 * I — Each opponent sacrifices a nontoken creature of their choice.
 * II — Investigate.
 * III — You may sacrifice an artifact or creature. If you do, create two 2/2 black Romulan
 * creature tokens.
 *
 * - Chapter III is an [Effects.MayPay] over the sacrifice: declining, or controlling no artifact
 *   or creature, creates nothing. The Clue from chapter II is natural fuel.
 */
val InThePaleMoonlight = card("In the Pale Moonlight") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Each opponent sacrifices a nontoken creature of their choice.\n" +
        "II — Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "III — You may sacrifice an artifact or creature. If you do, create two 2/2 black Romulan creature tokens."

    sagaChapter(1) {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.nontoken(),
            1,
            EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    sagaChapter(2) {
        effect = Effects.Investigate()
    }

    sagaChapter(3) {
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(GameObjectFilter.Artifact or GameObjectFilter.Creature),
            then = Effects.CreateToken(
                power = 2, toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Romulan"),
                count = 2,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Charles Chaisson"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47c8262c-e0c6-4c1a-b1c1-3b5fe5252a9c.jpg?1784065713"
    }
}
