package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zareth San, the Trickster — Zendikar Rising #242
 * {3}{U}{B} · Legendary Creature — Merfolk Rogue · 4/4
 *
 * Flash
 * {2}{U}{B}, Return an unblocked attacking Rogue you control to its owner's hand: Put this card from
 * your hand onto the battlefield tapped and attacking.
 * Whenever Zareth San deals combat damage to a player, you may put target permanent card from that
 * player's graveyard onto the battlefield under your control.
 *
 * - The hand ability is ninjutsu-shaped but **not ninjutsu**: no `Keyword.NINJUTSU` marker (Silver-Fur
 *   Master's discount and Satoru Umezawa's trigger don't see it), and — per the 2020-09-25 ruling —
 *   you choose what Zareth San attacks (CR 508.4), it needn't follow the returned Rogue. So the move
 *   is a pipeline over the card itself with `EnteringAttackDefender.CONTROLLER_CHOOSES`, filtered to
 *   `currentlyIn(Zone.HAND)` so it does nothing if Zareth San left your hand before resolution.
 * - The cost is only payable once blockers are declared (an attacker is neither blocked nor unblocked
 *   before then).
 * - "That player's graveyard" is the damaged player's — `ownedByTriggeringPlayer()`.
 */
val ZarethSanTheTrickster = card("Zareth San, the Trickster") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Merfolk Rogue"
    oracleText = "Flash\n" +
        "{2}{U}{B}, Return an unblocked attacking Rogue you control to its owner's hand: Put this card " +
        "from your hand onto the battlefield tapped and attacking.\n" +
        "Whenever Zareth San deals combat damage to a player, you may put target permanent card from " +
        "that player's graveyard onto the battlefield under your control."
    power = 4
    toughness = 4

    keywords(Keyword.FLASH)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}{B}"),
            Costs.ReturnToHand(GameObjectFilter.Creature.withSubtype("Rogue").attacking().unblocked()),
        )
        effect = Effects.Pipeline {
            val self = gather(CardSource.Entity(EffectTarget.Self))
            move(
                self,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.TappedAndAttacking),
                filter = GameObjectFilter.Any.currentlyIn(Zone.HAND),
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            )
        }
        activateFromZone = Zone.HAND
        description = "{2}{U}{B}, Return an unblocked attacking Rogue you control to its owner's hand: " +
            "Put this card from your hand onto the battlefield tapped and attacking."
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter(GameObjectFilter.Permanent.ownedByTriggeringPlayer(), zone = Zone.GRAVEYARD))
        effect = Effects.May(Effects.PutOntoBattlefieldUnderYourControl(t))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cfe4fbe-277f-410c-9eee-f7617f4cb850.jpg?1783929318"
        ruling("2020-09-25", "Before blockers are declared, attacking Rogues are neither blocked nor unblocked, so you can't pay the cost to activate Zareth San's second ability.")
        ruling("2020-09-25", "If Zareth San leaves your hand before its second ability resolves, the ability does nothing as it resolves. Zareth San remains in its new zone and the Rogue you returned remains in its owner's hand.")
        ruling("2020-09-25", "You choose which player or planeswalker Zareth San is attacking. It doesn't have to be attacking the same player or planeswalker that the returned Rogue was attacking.")
        ruling("2020-09-25", "Although Zareth San is attacking, it was never declared as an attacking creature (for purposes of abilities that trigger whenever a creature attacks, for example).")
    }
}
