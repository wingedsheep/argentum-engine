package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dire Fleet Ravager {3}{B}{B}
 * Creature — Orc Pirate Wizard
 * 4/4
 * Menace, deathtouch
 * When this creature enters, each player loses a third of their life, rounded up.
 *
 * [Effects.ForEachPlayer] rebinds the controller per player, so `lifeTotal(Player.You) divRoundedUp 3`
 * reads each player's *own* life total — the same shape Scourge of the Skyclaves uses for "half their
 * life, rounded up", with a divisor of 3 (13 life → loses 5; 1 life → loses 1).
 */
val DireFleetRavager = card("Dire Fleet Ravager") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Pirate Wizard"
    power = 4
    toughness = 4
    oracleText = "Menace, deathtouch\n" +
        "When this creature enters, each player loses a third of their life, rounded up."

    keywords(Keyword.MENACE, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effect = Effects.LoseLife(
                amount = DynamicAmounts.lifeTotal(Player.You) divRoundedUp 3,
                target = EffectTarget.Controller,
            ),
        )
        description = "When this creature enters, each player loses a third of their life, rounded up."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "104"
        artist = "Bram Sels"
        flavorText = "His fists fume with the decay of death, each strike a plague on the hearts of his enemies."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53bd7ccc-aa71-4a3f-a86e-936cb3b2cce3.jpg?1783935761"
        ruling("2017-09-29", "For example, if you have 13 life, you'll lose 5 life.")
        ruling(
            "2017-09-29",
            "If a player has 1 life, that player loses 1 life. If each player has 0 life after that, the game's a draw.",
        )
        ruling(
            "2017-09-29",
            "In a Two-Headed Giant game, each player loses a third of the team's life total rounded up. For " +
                "example, if a team has 13 life, each player on that team loses 5 life and the team loses 10 " +
                "life total.",
        )
    }
}
