package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akoum Firebird
 * {2}{R}{R}
 * Creature — Phoenix
 * 3/3
 * Flying, haste
 * This creature attacks each combat if able.
 * Landfall — Whenever a land you control enters, you may pay {4}{R}{R}. If you do, return this card
 * from your graveyard to the battlefield.
 *
 * The landfall ability only works from the graveyard ([triggerZone]); on the battlefield it does nothing.
 */
val AkoumFirebird = card("Akoum Firebird") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 3
    toughness = 3
    oracleText = "Flying, haste\n" +
        "This creature attacks each combat if able.\n" +
        "Landfall — Whenever a land you control enters, you may pay {4}{R}{R}. If you do, return this card " +
        "from your graveyard to the battlefield."

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = MustAttack()
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{4}{R}{R}"),
            then = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "138"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/304a2e4c-da2c-43fb-b44d-9db55ff94f04.jpg?1783938196"
    }
}
