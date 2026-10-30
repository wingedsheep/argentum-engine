package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sanctum of All — Core Set 2021 #225
 * {W}{U}{B}{R}{G} · Legendary Enchantment — Shrine
 *
 * At the beginning of your upkeep, you may search your library and/or graveyard for a Shrine card
 * and put it onto the battlefield. If you search your library this way, shuffle.
 * If a triggered ability of another Shrine you control triggers while you control six or more
 * Shrines, that ability triggers an additional time.
 *
 * The doubler is [AdditionalSourceTriggers] with `excludeSelf = true` ("another Shrine"); its
 * `condition` is evaluated as the ability triggers, so "while you control six or more Shrines" is
 * checked at trigger time, counting Sanctum of All itself.
 */
val SanctumOfAll = card("Sanctum of All") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your upkeep, you may search your library and/or graveyard for a Shrine " +
        "card and put it onto the battlefield. If you search your library this way, shuffle.\n" +
        "If a triggered ability of another Shrine you control triggers while you control six or more " +
        "Shrines, that ability triggers an additional time."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.withSubtype("Shrine"),
            destination = SearchDestination.BATTLEFIELD,
            optional = true
        )
        description = "At the beginning of your upkeep, you may search your library and/or graveyard for a " +
            "Shrine card and put it onto the battlefield. If you search your library this way, shuffle."
    }

    staticAbility {
        ability = AdditionalSourceTriggers(
            sourceFilter = GameObjectFilter.Any.withSubtype("Shrine"),
            excludeSelf = true,
            condition = Conditions.YouControlAtLeast(6, GameObjectFilter.Any.withSubtype("Shrine")),
            description = "If a triggered ability of another Shrine you control triggers while you control " +
                "six or more Shrines, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba91338c-1f6c-4b83-851f-98c3e9dea17b.jpg?1783930660"
        ruling(
            "2020-06-23",
            "If the Shrine you put onto the battlefield with Sanctum of All's first ability during your upkeep has an ability that triggers at the beginning of your upkeep, it won't trigger during that upkeep."
        )
        ruling(
            "2020-06-23",
            "Triggered abilities use the word \"when,\" \"whenever,\" or \"at.\" They're often written as \"[Trigger condition], [effect].\" Replacement effects, using \"instead\" or \"as,\" aren't affected. Activated abilities (written as \"Cost: Effect\") of Shrines you control are also not affected."
        )
    }
}
