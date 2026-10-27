package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Teeterpeak Ambusher — Zendikar Rising #169
 * {1}{R} · Creature — Goblin Warrior · 1/3
 *
 * {2}{R}: This creature gets +2/+0 until end of turn.
 */
val TeeterpeakAmbusher = card("Teeterpeak Ambusher") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "{2}{R}: This creature gets +2/+0 until end of turn."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "{2}{R}: This creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Kekai Kotaki"
        flavorText = "Goblins from the Makindi Trenches of Ondu make surprisingly skilled climbing guides " +
            "due to their habit of scaling the cliffs to drop onto unsuspecting foes."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad57c2f3-d9cb-4165-bb39-897b5b12689e.jpg"
    }
}
