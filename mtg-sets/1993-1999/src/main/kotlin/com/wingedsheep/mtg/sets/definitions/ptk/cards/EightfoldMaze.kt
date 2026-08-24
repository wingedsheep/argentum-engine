package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Eightfold Maze
 * {2}{W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Destroy target attacking creature.
 */
val EightfoldMaze = card("Eightfold Maze") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText =
        "Cast this spell only during the declare attackers step and only if you've been attacked this step.\n" +
        "Destroy target attacking creature."

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.attacking()))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Shang Huitong"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc8c377a-82c4-46ee-94c2-b970160a3205.jpg"
    }
}
