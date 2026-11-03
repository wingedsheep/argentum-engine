package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Anointed Deacon
 * {4}{B}
 * Creature — Vampire Cleric
 * 3/3
 * At the beginning of combat on your turn, you may have target Vampire get +2/+0 until end of turn.
 */
val AnointedDeacon = card("Anointed Deacon") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    power = 3
    toughness = 3
    oracleText = "At the beginning of combat on your turn, you may have target Vampire get +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val vampire = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Vampire")))
        effect = Effects.May(Effects.ModifyStats(2, 0, vampire))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Winona Nelson"
        flavorText = "Inspiration is red as rubies, flowing like honeyed wine."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bae5e75-0048-41fd-8bbb-89607ab96d6e.jpg?1783935768"
    }
}
