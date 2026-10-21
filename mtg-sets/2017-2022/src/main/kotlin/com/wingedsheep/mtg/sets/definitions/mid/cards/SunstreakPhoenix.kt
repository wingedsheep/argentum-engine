package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sunstreak Phoenix
 * {2}{R}{R}
 * Creature — Phoenix
 * 4/2
 * Flying
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, you may pay {1}{R}. If you do, return this card
 * from your graveyard to the battlefield tapped.
 *
 * The trigger moves the card out of the graveyard, so it functions only from there (CR 113.6m).
 */
val SunstreakPhoenix = card("Sunstreak Phoenix") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Phoenix"
    power = 4
    toughness = 2
    oracleText = "Flying\n" +
        "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, you may pay {1}{R}. If you do, return this card from your graveyard to the battlefield tapped."

    keywords(Keyword.FLYING)

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}{R}"),
            then = Effects.Move(
                EffectTarget.Self,
                Zone.BATTLEFIELD,
                placement = ZonePlacement.Tapped,
                fromZone = Zone.GRAVEYARD
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "162"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97de6e92-e0e8-444e-baba-09c97510b1ab.jpg?1783925590"
    }
}
