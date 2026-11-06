package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Renewal
 * {1}{W}
 * Enchantment
 *
 * Whenever a creature is put into your graveyard from the battlefield, you may sacrifice this
 * enchantment. If you do, return that card to the battlefield.
 *
 * "Put into your graveyard" is an ownership test, so the trigger watches creatures *you own* that die
 * — including one that died under another player's control (2018-12-07 ruling). The pay-then-payoff is
 * a [Effects.MayPay] with [SacrificeSelfEffect] as the cost (Servant of the Stinger), so an Angelic
 * Renewal that left the battlefield before resolution can't be sacrificed and returns nothing. The
 * return reads the triggering card out of the graveyard (`fromZone = GRAVEYARD`); a token that died has
 * already ceased to exist, so the move finds nothing, as the ruling says.
 */
val AngelicRenewal = card("Angelic Renewal") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature is put into your graveyard from the battlefield, you may sacrifice " +
        "this enchantment. If you do, return that card to the battlefield."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.ownedByYou()).dies()
        effect = Effects.MayPay(
            cost = SacrificeSelfEffect,
            then = Effects.Move(EffectTarget.TriggeringEntity, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
        )
        description = "Whenever a creature is put into your graveyard from the battlefield, you may " +
            "sacrifice this enchantment. If you do, return that card to the battlefield."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dddde7d-8565-45a7-a1db-f2dea2a6a3ba.jpg?1783946750"
        ruling("2018-12-07", "Angelic Renewal's ability triggers if a creature you own dies under another player's control.")
        ruling("2018-12-07", "If multiple creatures are put into your graveyard at once, Angelic Renewal triggers multiple times, but it can only be sacrificed once and only brings back one creature.")
        ruling("2018-12-07", "If Angelic Renewal leaves the battlefield before its triggered ability resolves, you won't be able to sacrifice it and the creature can't be returned to the battlefield.")
        ruling("2018-12-07", "If an animated permanent that's not normally a creature is put into your graveyard from the battlefield, Angelic Renewal can return that card to the battlefield.")
        ruling("2018-12-07", "If a token creature is put into your graveyard from the battlefield, you can sacrifice Angelic Renewal, but nothing will be returned to the battlefield.")
    }
}
