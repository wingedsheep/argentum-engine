package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Revel in Riches
 * {4}{B}
 * Enchantment
 *
 * Whenever a creature an opponent controls dies, create a Treasure token.
 * At the beginning of your upkeep, if you control ten or more Treasures, you win the game.
 *
 * The upkeep ability is an intervening "if" (CR 603.4): checked both when the step begins and on
 * resolution, per the rulings — the Hellkite Tyrant shape.
 */
val RevelInRiches = card("Revel in Riches") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature an opponent controls dies, create a Treasure token. (It's an " +
        "artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "At the beginning of your upkeep, if you control ten or more Treasures, you win the game."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.CreateTreasure()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControlAtLeast(10, GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE))
        effect = Effects.WinGame(
            target = EffectTarget.Controller,
            message = "Revel in Riches: ten or more Treasures."
        )
        description = "At the beginning of your upkeep, if you control ten or more Treasures, you win the game."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79b0e035-8716-469d-99ae-a530cd96ef09.jpg?1783935756"

        ruling(
            "2017-09-29",
            "If you don't control ten Treasures as your upkeep begins, the second ability of Revel in " +
                "Riches won't trigger. You can't take any actions during your turn before your upkeep begins."
        )
        ruling(
            "2017-09-29",
            "If you don't control ten Treasures as the second ability of Revel in Riches resolves, you won't win the game."
        )
        ruling(
            "2017-09-29",
            "If an opponent's creature dies at the same time that Revel in Riches is destroyed, you'll get a Treasure."
        )
    }
}
