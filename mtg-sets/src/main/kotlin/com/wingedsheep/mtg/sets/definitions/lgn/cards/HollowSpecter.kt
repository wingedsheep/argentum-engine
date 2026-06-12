package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayPayXForEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Hollow Specter
 * {1}{B}{B}
 * Creature — Specter
 * 2/2
 * Flying
 * Whenever Hollow Specter deals combat damage to a player, you may pay {X}.
 * If you do, that player reveals X cards from their hand and you choose one of them.
 * That player discards that card.
 */
val HollowSpecter = card("Hollow Specter") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever Hollow Specter deals combat damage to a player, you may pay {X}. If you do, that player reveals X cards from their hand and you choose one of them. That player discards that card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayPayXForEffect(
            effect = Effects.Pipeline {
                // 1. Gather all cards from damaged player's hand
                val hand = gather(
                    CardSource.FromZone(Zone.HAND, Player.TriggeringPlayer),
                    name = "hand"
                )
                // 2. Damaged player chooses X cards to reveal
                val revealed = chooseExactly(
                    DynamicAmount.XValue, from = hand,
                    chooser = Chooser.TriggeringPlayer,
                    name = "revealed"
                )
                // 3. Controller chooses 1 card to discard
                val toDiscard = chooseExactly(
                    1, from = revealed,
                    chooser = Chooser.Controller,
                    name = "toDiscard"
                )
                // 4. Move chosen card to damaged player's graveyard
                move(
                    toDiscard,
                    CardDestination.ToZone(Zone.GRAVEYARD, Player.TriggeringPlayer),
                    moveType = MoveType.Discard
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2db779fd-0e01-417b-aee2-786db2c0b8c8.jpg?1562904303"
        ruling("2004-10-04", "You decide on the value of X and pay {X} during resolution.")
        ruling("2004-10-04", "X can be zero, but then that player discards nothing.")
    }
}
