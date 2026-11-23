package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rakdos Ringleader — Return to Ravnica #186
 * {4}{B}{R} · Creature — Skeleton Warrior · Uncommon · 3/1
 *
 * First strike
 * Whenever this creature deals combat damage to a player, that player discards a card at random.
 * {B}: Regenerate this creature.
 */
val RakdosRingleader = card("Rakdos Ringleader") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Skeleton Warrior"
    power = 3
    toughness = 1
    oracleText = "First strike\n" +
        "Whenever this creature deals combat damage to a player, that player discards a card at random.\n" +
        "{B}: Regenerate this creature."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Hand.discardRandom(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b54fbe8-324a-4066-bed1-dda1dca319fc.jpg?1783940334"
    }
}
