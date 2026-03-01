package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Vexing Beetle
 * {4}{G}
 * Creature — Insect
 * 3/3
 * This spell can't be countered.
 * Vexing Beetle gets +3/+3 as long as no opponent controls a creature.
 */
val VexingBeetle = card("Vexing Beetle") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 3

    cantBeCountered = true

    staticAbility {
        ability = ModifyStats(3, 3, StaticTarget.SourceCreature)
        condition = Conditions.Not(Conditions.OpponentControlsCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Matt Thompson"
        flavorText = "\"As the Mirari's mutating effects grew out of control, centaurs and druids fled—but insects swarmed closer.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d599d35f-1b73-498b-9a21-831c908a95d8.jpg?1562937848"
    }
}
