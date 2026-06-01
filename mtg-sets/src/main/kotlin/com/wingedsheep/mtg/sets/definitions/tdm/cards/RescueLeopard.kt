package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Rescue Leopard — Tarkir: Dragonstorm #116
 * {2}{R} · Creature — Cat · 4/2
 *
 * Whenever this creature becomes tapped, you may discard a card. If you do, draw a card.
 */
val RescueLeopard = card("Rescue Leopard") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cat"
    power = 4
    toughness = 2
    oracleText = "Whenever this creature becomes tapped, you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = EffectPatterns.discardCards(1),
                ifYouDo = Effects.DrawCards(1)
            ),
            descriptionOverride = "You may discard a card. If you do, draw a card."
        )
        description = "Whenever this creature becomes tapped, you may discard a card. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Hector Ortiz"
        flavorText = "\"The scouts should have been back hours ago. They might be in trouble. " +
            "I'm sending Kyra.\"\n—Eshki Dragonclaw"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/056136a8-84be-477c-b654-63238fb8236e.jpg?1743204430"
    }
}
