package com.wingedsheep.mtg.sets.definitions.sok.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kami of the Crescent Moon
 * {U}{U}
 * Legendary Creature — Spirit
 * 1/3
 * At the beginning of each player's draw step, that player draws an additional card.
 *
 * Rites of Flourishing's draw trigger: it fires on every player's draw step and resolves after the
 * turn's normal draw.
 */
val KamiOfTheCrescentMoon = card("Kami of the Crescent Moon") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Spirit"
    oracleText = "At the beginning of each player's draw step, that player draws an additional card."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Darrell Riche"
        flavorText = "\"He's a lot like me, that masterless little kami . . . unimpressed by grandeur and never at a loss for a trick.\"\n—Toshiro Umezawa"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f01ee008-76dd-4d4d-8273-ad5b28c8a2c7.jpg?1783944163"
        ruling("2005-06-01", "The additional draw triggers just after the normal draw for the turn happens.")
    }
}
