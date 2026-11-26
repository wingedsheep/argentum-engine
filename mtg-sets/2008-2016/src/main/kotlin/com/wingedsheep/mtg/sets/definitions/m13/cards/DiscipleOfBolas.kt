package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Disciple of Bolas — Magic 2013 #88 (canonical printing)
 * {3}{B} · Creature — Human Wizard · 2/1
 *
 * When this creature enters, sacrifice another creature. You gain X life and draw X cards, where
 * X is that creature's power.
 *
 * The sacrifice is mandatory and happens on resolution: [Effects.SacrificeOwn] with
 * `excludeSource` keeps the Disciple itself off the list ("another creature"). The sacrifice
 * records a last-known-information snapshot (CR 608.2h) that the composite carries forward, so
 * [DynamicAmounts.totalPowerSacrificedThisWay] reads the creature's power as it last existed on
 * the battlefield — anthems and pumps included. With no other creature, nothing is sacrificed and
 * X is 0.
 */
val DiscipleOfBolas = card("Disciple of Bolas") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, sacrifice another creature. You gain X life and draw " +
        "X cards, where X is that creature's power."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true) then
            Effects.GainLife(DynamicAmounts.totalPowerSacrificedThisWay()) then
            Effects.DrawCards(DynamicAmounts.totalPowerSacrificedThisWay())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Slawomir Maniak"
        flavorText = "\"Lord Bolas has shown me how each miserable little life holds untold resources.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4dd57f8-27bc-4ad9-a79e-48a68af33b02.jpg"
    }
}
