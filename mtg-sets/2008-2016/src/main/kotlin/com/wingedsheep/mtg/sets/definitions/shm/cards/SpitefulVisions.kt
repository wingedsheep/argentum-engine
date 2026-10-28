package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spiteful Visions
 * {2}{B/R}{B/R}
 * Enchantment
 * At the beginning of each player's draw step, that player draws an additional card.
 * Whenever a player draws a card, this enchantment deals 1 damage to that player.
 */
val SpitefulVisions = card("Spiteful Visions") {
    manaCost = "{2}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's draw step, that player draws an additional card.\n" +
        "Whenever a player draws a card, this enchantment deals 1 damage to that player."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.draws()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Brandon Kitkouski"
        flavorText = "Knowledge brings the sting of disillusionment, but the pain teaches perspective."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19039ac4-1aaa-4aca-ba09-f7fc400b9ada.jpg?1783942725"
        ruling("2013-04-15", "The triggered ability is put onto the stack after you have already drawn your card for the turn.")
        ruling(
            "2008-05-01",
            "Spiteful Visions's second ability triggers any time any player draws a card, not just when a " +
                "player draws a card as a result of its first ability."
        )
    }
}
