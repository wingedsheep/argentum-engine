package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hour of Devastation
 * {3}{R}{R}
 * Sorcery
 * All creatures lose indestructible until end of turn. Hour of Devastation deals 5 damage to each
 * creature and each non-Bolas planeswalker.
 *
 * The keyword loss is a per-creature floating effect over the creatures on the battlefield at
 * resolution (later arrivals keep indestructible), applied before the damage sweep, so the
 * state-based check after resolution destroys formerly indestructible creatures. Planeswalkers
 * don't lose indestructible — only creatures do.
 */
val HourOfDevastation = card("Hour of Devastation") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "All creatures lose indestructible until end of turn. Hour of Devastation deals 5 damage to each " +
        "creature and each non-Bolas planeswalker."

    spell {
        effect = Patterns.Group.removeKeywordFromAll(Keyword.INDESTRUCTIBLE, Filters.Group.allCreatures) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature or GameObjectFilter.Planeswalker.notSubtype(Subtype("Bolas"))),
                Effects.DealDamage(5, EffectTarget.IterationEntity),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Simon Dominic"
        flavorText = "\"Everything here exists or perishes at my whim. Including you, Gatewatch.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d420cc12-cfd7-4007-a0c2-b16c8f63a754.jpg?1783936028"
        ruling("2017-07-14", "Creatures with indestructible that enter the battlefield after Hour of Devastation resolves will still have indestructible.")
        ruling("2017-07-14", "Once Hour of Devastation begins to resolve, no player may take actions until it's done. Notably, players can't try to give a creature indestructible again to save it from the damage.")
    }
}
