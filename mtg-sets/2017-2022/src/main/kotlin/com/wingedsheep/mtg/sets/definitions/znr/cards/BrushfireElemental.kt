package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brushfire Elemental — Zendikar Rising #221
 * {R}{G} · Creature — Elemental · 1/1
 *
 * Haste
 * This creature can't be blocked by creatures with power 2 or less.
 * Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn.
 */
val BrushfireElemental = card("Brushfire Elemental") {
    manaCost = "{R}{G}"
    colorIdentity = "GR"
    typeLine = "Creature — Elemental"
    oracleText = "Haste\n" +
        "This creature can't be blocked by creatures with power 2 or less.\n" +
        "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."
    power = 1
    toughness = 1

    keywords(Keyword.HASTE)

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fc448f6-a7fe-47c4-ac7c-385a77326469.jpg"
    }
}
