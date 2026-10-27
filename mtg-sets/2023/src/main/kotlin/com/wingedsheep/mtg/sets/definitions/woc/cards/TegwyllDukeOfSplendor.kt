package com.wingedsheep.mtg.sets.definitions.woc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tegwyll, Duke of Splendor
 * {1}{U}{B}
 * Legendary Creature — Faerie Noble
 * 2/3
 * Flying, deathtouch
 * Other Faeries you control get +1/+1.
 * Whenever another Faerie you control dies, you draw a card and you lose 1 life.
 */
val TegwyllDukeOfSplendor = card("Tegwyll, Duke of Splendor") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Faerie Noble"
    oracleText = "Flying, deathtouch\n" +
        "Other Faeries you control get +1/+1.\n" +
        "Whenever another Faerie you control dies, you draw a card and you lose 1 life."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Faerie").youControl(),
                excludeSelf = true,
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype("Faerie").youControl()).dies()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Ekaterina Burmak"
        flavorText = "He weaves glamours as easily as breathing, his otherworldly beauty entrancing fae and mortal alike."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03fab911-7bd4-45fa-ba07-cd0e51b0cd95.jpg?1783914991"

        ruling(
            "2023-09-01",
            "If Tegwyll, Duke of Splendor dies at the same time as one or more other Faeries you control, " +
                "Tegwyll's ability triggers for each of those other Faeries."
        )
    }
}
