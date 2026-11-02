package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Make It So
 * {3}{U}
 * Sorcery
 *
 * Choose one —
 * • Untap target permanent you control. Target player draws two cards.
 * • Create a 1/1 red Officer creature token and draw two cards.
 */
val MakeItSo = card("Make It So") {
    manaCost = "{3}{U}"
    typeLine = "Sorcery"
    colorIdentity = "U"
    oracleText = "Choose one —\n• Untap target permanent you control. Target player draws two cards.\n" +
        "• Create a 1/1 red Officer creature token and draw two cards."
    spell {
        effect = Effects.Modal(listOf(
            mode("Untap target permanent you control. Target player draws two cards") {
                val permanent = target(TargetFilter.PermanentYouControl)
                val player = target(Targets.Player)
                effect = Effects.Untap(permanent) then Effects.DrawCards(2, player)
            },
            mode("Create a 1/1 red Officer creature token and draw two cards") {
                effect = Effects.CreateToken(
                    power = 1, toughness = 1, colors = setOf(Color.RED), creatureTypes = setOf("Officer")
                ) then Effects.DrawCards(2)
            }
        ))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7149df19-4833-47ca-8add-b897f55b930c.jpg?1793013541"
    }
}
