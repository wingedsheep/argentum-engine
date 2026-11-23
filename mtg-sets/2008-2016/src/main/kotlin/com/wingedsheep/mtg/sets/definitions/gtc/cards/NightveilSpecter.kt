package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nightveil Specter
 * {U/B}{U/B}{U/B}
 * Creature — Specter
 * 2/3
 *
 * Flying
 * Whenever this creature deals combat damage to a player, that player exiles the top card of their
 * library.
 * You may play lands and cast spells from among cards exiled with this creature.
 *
 * Rogue Class's level-1/level-3 pairing on one creature: the combat-damage trigger moves the
 * damaged player's ([Player.TriggeringPlayer]) top card to exile face up, linked to this Specter
 * (`linkToSource`, CR 607 linked abilities), and a `MayPlayCardsFromExile` static over the Specter's
 * own pile (`exiledWithSource`) makes those cards playable — lands included ("play lands"), under
 * the normal timing and land-drop rules. Being a static of the Specter, the permission lasts only
 * while it's on the battlefield and belongs to its current controller, and each Specter tracks only
 * its own pile (2013-01-24 ruling). The current Oracle text has no mana-of-any-color clause.
 */
val NightveilSpecter = card("Nightveil Specter") {
    manaCost = "{U/B}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Creature — Specter"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, that player exiles the top card " +
        "of their library.\n" +
        "You may play lands and cast spells from among cards exiled with this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, player = Player.TriggeringPlayer))
            move(
                top,
                CardDestination.ToZone(Zone.EXILE, Player.TriggeringPlayer),
                linkToSource = true
            )
        }
        description = "Whenever this creature deals combat damage to a player, that player exiles " +
            "the top card of their library."
    }

    staticAbility {
        ability = MayPlayCardsFromExile(filter = GameObjectFilter.Any.exiledWithSource())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3754b8c-16d2-41e3-b41b-4b2e70833e82.jpg?1783940094"

        ruling("2013-01-24", "The card is exiled face up. All players may look at it.")
        ruling("2013-01-24", "Playing a card exiled with Nightveil Specter follows all the normal " +
            "rules for playing that card. You must pay its costs, and you must follow all timing " +
            "restrictions, for example.")
        ruling("2013-01-24", "Nightveil Specter's last ability applies to cards exiled with that " +
            "specific Nightveil Specter, not any other creature named Nightveil Specter. You should " +
            "keep cards exiled by different Nightveil Specters separate.")
    }
}
