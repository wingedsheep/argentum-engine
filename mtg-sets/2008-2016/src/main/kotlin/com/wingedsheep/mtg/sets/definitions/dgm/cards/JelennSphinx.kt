package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jelenn Sphinx
 * {3}{W}{U}
 * Creature — Sphinx
 * 1/5
 * Flying, vigilance
 * Whenever this creature attacks, other attacking creatures get +1/+1 until end of turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val JelennSphinx = card("Jelenn Sphinx") {
    manaCost = "{3}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Sphinx"
    power = 1
    toughness = 5
    oracleText = "Flying, vigilance\n" +
        "Whenever this creature attacks, other attacking creatures get +1/+1 until end of turn."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter.AttackingCreatures.other(),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Wesley Burt"
        flavorText = "Few among the Azorius have ever spoken with the sphinxes, but all have felt their influence."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/533c89eb-d7c6-4945-9689-2f2c0e428b84.jpg"
    }
}
