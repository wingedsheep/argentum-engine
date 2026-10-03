package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Azula, On the Hunt
 * {3}{B}
 * Legendary Creature — Human Noble
 * 4/3
 *
 * Firebending 2 (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)
 * Whenever Azula attacks, you lose 1 life and create a Clue token. (It's an artifact with
 * "{2}, Sacrifice this token: Draw a card.")
 *
 * Firebending 2 is the set keyword combat-mana helper. The attack trigger composes
 * `Effects.LoseLife` (the controller, Player.You) with `Effects.CreateClue` (which mints the
 * standard Clue token), both firing whenever Azula attacks.
 */
val AzulaOnTheHunt = card("Azula, On the Hunt") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Noble"
    power = 4
    toughness = 3
    oracleText = "Firebending 2 (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)\n" +
        "Whenever Azula attacks, you lose 1 life and create a Clue token. (It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    firebending(2)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)) then Effects.CreateClue()
        description = "Whenever Azula attacks, you lose 1 life and create a Clue token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Toraji"
        flavorText = "\"You can run, but I'll catch you.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1335a145-248a-4f1e-8760-9a5d531e14e3.jpg?1764120589"
    }
}
