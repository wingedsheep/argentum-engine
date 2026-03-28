package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valley Floodcaller
 * {2}{U}
 * Creature — Otter Wizard
 * 2/2
 *
 * Flash
 * You may cast noncreature spells as though they had flash.
 * Whenever you cast a noncreature spell, Birds, Frogs, Otters, and Rats you control
 * get +1/+1 until end of turn. Untap them.
 */
val ValleyFloodcaller = card("Valley Floodcaller") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Otter Wizard"
    power = 2
    toughness = 2
    oracleText = "Flash\nYou may cast noncreature spells as though they had flash.\nWhenever you cast a noncreature spell, Birds, Frogs, Otters, and Rats you control get +1/+1 until end of turn. Untap them."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Noncreature,
            controllerOnly = true
        )
    }

    val creatureFilter = GroupFilter(
        GameObjectFilter.Creature
            .withAnyOfSubtypes(
                listOf(
                    Subtype("Bird"),
                    Subtype("Frog"),
                    Subtype("Otter"),
                    Subtype("Rat")
                )
            )
            .youControl()
    )

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = ForEachInGroupEffect(
            filter = creatureFilter,
            effect = CompositeEffect(
                listOf(
                    ModifyStatsEffect(1, 1, EffectTarget.Self),
                    TapUntapEffect(EffectTarget.Self, tap = false)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90b12da0-f666-471d-95f5-15d8c9b31c92.jpg?1721639406"
    }
}
