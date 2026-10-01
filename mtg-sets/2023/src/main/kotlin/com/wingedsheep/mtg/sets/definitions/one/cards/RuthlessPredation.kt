package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ruthless Predation — Phyrexia: All Will Be One #182
 * {1}{G}
 * Sorcery
 *
 * Target creature you control gets +1/+2 until end of turn. It fights target creature you
 * don't control.
 *
 * The pump resolves before the fight, so the bonus counts toward both the damage dealt and the
 * damage survived.
 */
val RuthlessPredation = card("Ruthless Predation") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +1/+2 until end of turn. It fights target creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(1, 2, mine) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Pavel Kolomeyets"
        flavorText = "In the Hunter Maze, today's apex predator is tomorrow's prey."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/123f0c76-1fde-439e-a76d-eccf96f8d941.jpg?1783918010"
    }
}
