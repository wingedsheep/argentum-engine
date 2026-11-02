package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Thief of Sanity
 * {1}{U}{B}
 * Creature — Specter
 * 2/2
 *
 * Flying
 * Whenever this creature deals combat damage to a player, look at the top three cards of that
 * player's library, exile one of them face down, then put the rest into their graveyard. You may
 * cast that card for as long as it remains exiled, and mana of any type can be spent to cast that
 * spell.
 *
 * The Black Cat, Cunning Thief pipeline off the damaged player's library
 * ([Player.TriggeringPlayer] for a SELF-bound combat-damage trigger, as Ragavan uses): gather the
 * top three, choose exactly one to exile face down ([FaceDownMode.HIDDEN]), the remainder to that
 * player's graveyard, then a `Permanent` ("for as long as it remains exiled") may-play grant with
 * `withAnyManaType`. `nonLandOnly` carries the "cast" wording — a land exiled this way can't be
 * played (2018-10-05 ruling). The grant goes to the ability's controller at resolution, so it
 * survives Thief of Sanity leaving play or changing control, as the rulings require.
 */
val ThiefOfSanity = card("Thief of Sanity") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Specter"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, look at the top three cards of " +
        "that player's library, exile one of them face down, then put the rest into their " +
        "graveyard. You may cast that card for as long as it remains exiled, and mana of any " +
        "type can be spent to cast that spell."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val topThree = gather(
                CardSource.TopOfLibrary(
                    count = 3,
                    player = Player.TriggeringPlayer,
                )
            )
            val (exiled, rest) = chooseExactlySplit(
                1,
                from = topThree,
                chooser = Chooser.Controller,
                prompt = "Exile one of those cards face down.",
                showAllCards = true
            )
            exile(exiled, Player.TriggeringPlayer, faceDown = FaceDownMode.HIDDEN)
            move(rest, CardDestination.ToZone(Zone.GRAVEYARD, Player.TriggeringPlayer))
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.Permanent,
                withAnyManaType = true,
                nonLandOnly = true,
            ))
        }
        description = "Whenever this creature deals combat damage to a player, look at the top " +
            "three cards of that player's library, exile one of them face down, then put the rest " +
            "into their graveyard. You may cast that card for as long as it remains exiled, and " +
            "mana of any type can be spent to cast that spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/307543ca-8e17-433f-9758-4c77da6c0870.jpg?1783934119"

        ruling("2018-10-05", "Thief of Sanity's effect doesn't change when you can cast the " +
            "exiled card. For example, if you exile a sorcery card, you can cast it only during " +
            "your main phase when the stack is empty.")
        ruling("2018-10-05", "An effect that instructs you to \"cast\" a card doesn't allow you " +
            "to play lands.")
        ruling("2018-10-05", "You can still cast the exiled card if Thief of Sanity leaves the " +
            "battlefield or leaves your control. If another player gains control of Thief of " +
            "Sanity, that player can't cast the exiled card.")
        ruling("2018-10-05", "Casting an exiled card causes it to leave exile. You can't cast it " +
            "multiple times.")
    }
}
