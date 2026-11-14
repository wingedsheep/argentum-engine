package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hydrolash
 * {2}{U}
 * Instant
 *
 * Attacking creatures get -2/-0 until end of turn.
 * Draw a card.
 *
 * "Attacking creatures" is every attacker, whoever controls it — not a target — so this is
 * [Effects.ForEachInGroup] over [GroupFilter.AttackingCreatures]. The group is locked in at
 * resolution; a creature that starts attacking later is unaffected. With no attackers it still
 * draws a card (2015-06-22 ruling).
 */
val Hydrolash = card("Hydrolash") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Attacking creatures get -2/-0 until end of turn.\nDraw a card."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AttackingCreatures,
            Effects.ModifyStats(-2, 0, EffectTarget.IterationEntity)
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Clint Cearley"
        flavorText = "\"I don't need your mage-rings or your mana equations. I call, and the sea answers.\"\n—Keyta, rebel hydromancer"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeaaa583-bdd3-448a-921e-527df5f7f548.jpg?1783938351"
        ruling("2015-06-22", "You may cast Hydrolash even if there aren't any attacking creatures simply to draw a card.")
    }
}
