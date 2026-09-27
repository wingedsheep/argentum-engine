package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zo-Zu the Punisher
 * {1}{R}{R}
 * Legendary Creature — Goblin Warrior
 * 2/2
 * Whenever a land enters, Zo-Zu deals 2 damage to that land's controller.
 *
 * Symmetric: any land entering under any player's control fires it, and the damage goes to the
 * entering land's controller ([EffectTarget.ControllerOfTriggeringEntity]) — Zo-Zu's own
 * controller included. No target, so hexproof/shroud on the player doesn't stop it.
 */
val ZoZuThePunisher = card("Zo-Zu the Punisher") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Warrior"
    oracleText = "Whenever a land enters, Zo-Zu deals 2 damage to that land's controller."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land).enters()
        effect = Effects.DealDamage(2, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Matt Cavotta"
        flavorText = "\"He can cause a lot of pain and do it with no fuss. That's all good, but I just " +
            "wish he didn't do it to us!\"\n—Ku-Ku, akki poet"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa4666b6-76f2-4c25-989e-d6a0e50be94d.jpg?1783944293"
    }
}
