package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Heavy Infantry
 * {4}{W}
 * Creature — Human Soldier
 * 3/4
 * When this creature enters, tap target creature an opponent controls.
 */
val HeavyInfantry = card("Heavy Infantry") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, tap target creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "David Gaillet"
        flavorText = "Doors, walls, skulls . . . it matters not. All barriers will be broken."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b904b1c-bf35-4bc1-8022-7f632160733d.jpg?1783938361"
    }
}
