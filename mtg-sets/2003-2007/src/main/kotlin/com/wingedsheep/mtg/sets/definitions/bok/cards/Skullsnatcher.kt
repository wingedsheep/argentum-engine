package com.wingedsheep.mtg.sets.definitions.bok.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skullsnatcher — Betrayers of Kamigawa #84 (canonical printing)
 * {1}{B} · Creature — Rat Ninja · 2/1
 *
 * Ninjutsu {B}
 * Whenever this creature deals combat damage to a player, exile up to two target cards from that
 * player's graveyard.
 *
 * "That player's graveyard" is the damaged player's — `ownedByTriggeringPlayer()` (Froghemoth's
 * spelling); "up to two" is `count = 2, optional = true` (zero targets is legal, 2005-02-01 ruling),
 * and the payoff exiles each chosen target, as on Rag Dealer.
 */
val Skullsnatcher = card("Skullsnatcher") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Ninja"
    power = 2
    toughness = 1
    oracleText = "Ninjutsu {B} ({B}, Return an unblocked attacker you control to hand: Put this card " +
        "onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, exile up to two target cards from " +
        "that player's graveyard."

    ninjutsu("{B}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        targets(TargetFilter.CardInGraveyard.ownedByTriggeringPlayer(), count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cb76fa1-a930-468d-bfd4-dd06c2a9fe9e.jpg?1783944195"
        ruling("2005-02-01", "Skullsnatcher's ability can target zero, one, or two cards in that player's graveyard.")
    }
}
