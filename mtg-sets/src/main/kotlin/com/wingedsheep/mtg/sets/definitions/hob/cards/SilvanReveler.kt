package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silvan Reveler
 * {2}{G}{U}
 * Creature — Elf Citizen
 * 3/2
 * When this creature enters, draw a card, then discard a card. If you discard a land card this way,
 * put it from your graveyard onto the battlefield tapped.
 * Landfall — Whenever a land you control enters, you may pay {1}{G}{U}. If you do, return this card
 * from your graveyard to your hand.
 *
 *  - **The discard is a real discard** ([MoveType.Discard]), not a plain move to the graveyard, so
 *    discard triggers and madness see it. Madness is exactly why the land branch re-checks the zone:
 *    a discarded land with madness is exiled instead of hitting the graveyard, and "put it **from
 *    your graveyard**" then has nothing to put. The [CollectionFilter.InZone] step keeps that case
 *    a no-op rather than dragging the card back out of exile.
 *  - Putting the land onto the battlefield is not playing a land (CR 305.1), so it neither uses nor
 *    needs a land drop.
 *  - **The landfall ability functions from the graveyard** (`triggerZone = Zone.GRAVEYARD`,
 *    CR 113.6b) — that is the only zone it does anything in. "You" is the card's owner there, so
 *    `Land.youControl()` correctly watches the owner's lands.
 */
val SilvanReveler = card("Silvan Reveler") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Citizen"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, draw a card, then discard a card. If you discard a " +
        "land card this way, put it from your graveyard onto the battlefield tapped.\n" +
        "Landfall — Whenever a land you control enters, you may pay {1}{G}{U}. If you do, return " +
        "this card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline {
            // "draw a card, then discard a card"
            run(Effects.DrawCards(1))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseExactly(
                1,
                from = hand,
                prompt = "Choose a card to discard",
                selectedLabel = "Discard"
            )
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Discard)
            // "If you discard a land card this way, put it from your graveyard onto the
            // battlefield tapped."
            val discardedLand = filter(discarded, GameObjectFilter.Land)
            val landInGraveyard = filter(discardedLand, CollectionFilter.InZone(Zone.GRAVEYARD))
            move(
                landInGraveyard,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped)
            )
        }
        description = "When this creature enters, draw a card, then discard a card. If you " +
            "discard a land card this way, put it from your graveyard onto the battlefield tapped."
    }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}{G}{U}"),
            effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        )
        description = "Landfall — Whenever a land you control enters, you may pay {1}{G}{U}. If " +
            "you do, return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Bokun An"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c71b74fb-fb0c-4953-b536-7a3f283c6918.jpg?1784382379"
    }
}
