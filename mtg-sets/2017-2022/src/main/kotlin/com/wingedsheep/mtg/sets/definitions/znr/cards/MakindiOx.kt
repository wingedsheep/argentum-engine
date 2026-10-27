package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Makindi Ox — Zendikar Rising #25
 * {4}{W} · Creature — Ox · 4/4
 *
 * Landfall — Whenever a land you control enters, tap target creature an opponent controls.
 */
val MakindiOx = card("Makindi Ox") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Ox"
    oracleText = "Landfall — Whenever a land you control enters, tap target creature an opponent controls."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(t)
        description = "Landfall — Whenever a land you control enters, tap target creature an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Ilse Gort"
        flavorText = "\"May bloodflies empty your veins and turn you into a dried leather sack, cow!\"\n" +
            "—Bruse Tarl, Goma Fada nomad"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd0d164a-bce2-4474-abe0-b735eb6c9442.jpg"
    }
}
