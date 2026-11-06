package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Midnight Entourage
 * {2}{B}{B}
 * Creature — Aetherborn Rogue
 * 3/3
 * Other Aetherborn you control get +1/+1.
 * Whenever this creature or another Aetherborn you control dies, you draw a card and you lose 1 life.
 */
val MidnightEntourage = card("Midnight Entourage") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Rogue"
    oracleText = "Other Aetherborn you control get +1/+1.\n" +
        "Whenever this creature or another Aetherborn you control dies, you draw a card and you lose 1 life."
    power = 3
    toughness = 3

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype("Aetherborn")).youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype("Aetherborn")).youControl()).dies()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2838c6dd-d816-4363-a861-f2f8052d1430.jpg"
        ruling("2017-02-09", "Midnight Entourage's triggered ability is mandatory. You draw a card and lose 1 life even if you don't want to.")
        ruling("2017-02-09", "If Midnight Entourage and another Aetherborn you control die at the same time, its ability triggers once for each of them.")
        ruling("2017-02-09", "If a creature has been dealt damage, that damage remains marked on it until the cleanup step. If Midnight Entourage leaves the battlefield and another Aetherborn creature you control has been dealt damage, that creature will be destroyed if the damage is now lethal. The same is true if an Aetherborn's toughness becomes 0 this way.")
    }
}
