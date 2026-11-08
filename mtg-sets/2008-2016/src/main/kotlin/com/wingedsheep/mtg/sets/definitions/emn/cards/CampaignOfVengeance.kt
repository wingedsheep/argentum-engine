package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Campaign of Vengeance
 * {3}{W}{B}
 * Enchantment
 * Whenever a creature you control attacks, defending player loses 1 life and you gain 1 life.
 *
 * One trigger per attacking creature; each drains the player that creature attacks (the
 * controller of the planeswalker it attacks), so attackers split across opponents drain each of
 * them separately — the same shape as Brutal Hordechief's trigger.
 */
val CampaignOfVengeance = card("Campaign of Vengeance") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks, defending player loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.DefendingPlayer)) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Igor Kieryluk"
        flavorText = "\"There is no saving this world, not anymore. But we shall be its bloody vengeance.\"\n—Sorin Markov"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c284997a-cfc3-4ffc-bc25-00c130fb1eb0.jpg?1783937432"
        ruling("2016-07-13", "If a creature attacks a planeswalker, the defending player is the player who controls that planeswalker.")
        ruling("2016-07-13", "In a multiplayer game, each instance of the ability affects only one defending player. For example, if you attack player A with two creatures and player B with two creatures, each of those players will lose 2 life, not 4.")
    }
}
