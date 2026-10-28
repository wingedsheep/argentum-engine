package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nekusar, the Mindrazer
 * {2}{U}{B}{R}
 * Legendary Creature — Zombie Wizard
 * 2/4
 * At the beginning of each player's draw step, that player draws an additional card.
 * Whenever an opponent draws a card, Nekusar deals 1 damage to that player.
 */
val NekusarTheMindrazer = card("Nekusar, the Mindrazer") {
    manaCost = "{2}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Zombie Wizard"
    oracleText = "At the beginning of each player's draw step, that player draws an additional card.\n" +
        "Whenever an opponent draws a card, Nekusar deals 1 damage to that player."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "201"
        artist = "Mark Winters"
        flavorText = "His enemies wondered if the lich king's brutal death and unnatural rebirth had been his plan all along."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f7f2417-12de-4e57-9714-d878880a1208.jpg?1783939648"
        ruling(
            "2020-11-10",
            "If a spell or ability causes you to put cards into your hand without specifically using the word " +
                "\"draw,\" it doesn't count as drawing cards and it doesn't cause Nekusar's second ability to trigger."
        )
    }
}
