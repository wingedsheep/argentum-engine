package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Keiga, the Tide Star
 * {5}{U}
 * Legendary Creature — Dragon Spirit
 * 5/5
 * Flying
 * When Keiga dies, gain control of target creature.
 *
 * `Triggers.self.dies()` with a creature target feeding [Effects.GainControl] at its default
 * permanent duration — the control change never wears off (Scryfall ruling 2017-11-17).
 */
val KeigaTheTideStar = card("Keiga, the Tide Star") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Keiga dies, gain control of target creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature)
        description = "When Keiga dies, gain control of target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Ittoku"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea83eaeb-cf82-406b-977f-2bac41925739.jpg?1783944325"
        ruling("2017-11-17", "Keiga's effect lasts indefinitely. It doesn't wear off during the cleanup step.")
    }
}
