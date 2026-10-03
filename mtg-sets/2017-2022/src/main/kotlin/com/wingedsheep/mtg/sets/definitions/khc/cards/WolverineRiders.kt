package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wolverine Riders
 * {4}{G}{G}
 * Creature — Elf Warrior
 * 4/4
 * At the beginning of each upkeep, create a 1/1 green Elf Warrior creature token.
 * Whenever another Elf you control enters, you gain life equal to its toughness.
 *
 * The upkeep trigger is Verdant Force's `Triggers.anyPlayer.beginningOf(UPKEEP)`. "Another Elf"
 * is the bare tribal noun — any permanent with the subtype — and "its toughness" reads the
 * entering permanent through [DynamicAmounts.triggeringToughness].
 */
val WolverineRiders = card("Wolverine Riders") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Elf Warrior"
    power = 4
    toughness = 4
    oracleText = "At the beginning of each upkeep, create a 1/1 green Elf Warrior creature token.\n" +
        "Whenever another Elf you control enters, you gain life equal to its toughness."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior"),
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype("Elf").youControl()).enters()
        effect = Effects.GainLife(DynamicAmounts.triggeringToughness())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Jesper Ejsing"
        flavorText = "\"We'll break their lines. The rest of you, follow!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70fd0439-294b-454c-b2af-e814b85f4590.jpg?1783928337"
    }
}
