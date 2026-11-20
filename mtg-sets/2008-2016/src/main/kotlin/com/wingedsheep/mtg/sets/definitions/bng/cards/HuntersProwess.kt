package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hunter's Prowess
 * {4}{G}
 * Sorcery
 *
 * Until end of turn, target creature gets +3/+3 and gains trample and "Whenever this creature deals
 * combat damage to a player, draw that many cards."
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing. "That many" is the combat
 * damage the creature dealt to that player (`triggerDamageAmount`).
 */
val HuntersProwess = card("Hunter's Prowess") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, target creature gets +3/+3 and gains trample and \"Whenever this " +
        "creature deals combat damage to a player, draw that many cards.\""

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
                    effect = Effects.DrawCards(DynamicAmounts.triggerDamageAmount())
                    description = "Whenever this creature deals combat damage to a player, draw that many cards."
                },
                target = creature
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Greg Staples"
        flavorText = "To be counted among the warriors of Setessa's Leina Tower, you must not fear death, only failure."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cef2323c-3e35-4ac6-9f07-2c0584371f9e.jpg?1783939535"
    }
}
