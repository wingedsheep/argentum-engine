package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Imposing Sovereign — Magic 2014 #22
 * {1}{W} · Creature — Human Noble · 2/1
 *
 * Creatures your opponents control enter tapped.
 *
 * A global [PermanentsEnterTapped] replacement consulted from the battlefield whenever another
 * permanent enters — the same shape as Thalia, Heretic Cathar's creature half. Creatures entering
 * simultaneously with Imposing Sovereign are unaffected, since the replacement only exists once it
 * is on the battlefield.
 */
val ImposingSovereign = card("Imposing Sovereign") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Noble"
    power = 2
    toughness = 1
    oracleText = "Creatures your opponents control enter tapped."

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Scott M. Fischer"
        flavorText = "Some are born to rule. The rest are born to bow before them."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f672328-3361-498e-a9f4-2d8e69a8b072.jpg?1783939942"
    }
}
