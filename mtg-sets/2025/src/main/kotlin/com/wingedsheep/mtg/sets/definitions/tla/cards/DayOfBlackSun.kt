package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Day of Black Sun
 * {X}{B}{B}
 * Sorcery
 * Each creature with mana value X or less loses all abilities until end of turn.
 * Destroy those creatures.
 *
 * X is the {X} in the mana cost; it is surfaced to resolution as the spell's X value, so the
 * group filter ([GameObjectFilter.manaValueAtMostX]) matches
 * "creatures with mana value X or less" (cf. Vicious Rivalry's board wipe). The abilities are
 * stripped first via [Effects.ForEachInGroup] (the group is snapshotted before any sub-effect
 * applies), which removes indestructible/regeneration/etc. before [Effects.DestroyAll] resolves
 * over the same filter — so "those creatures" are reliably destroyed.
 */
val DayOfBlackSun = card("Day of Black Sun") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each creature with mana value X or less loses all abilities until end of turn. " +
        "Destroy those creatures."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.manaValueAtMostX()),
            effect = Effects.RemoveAllAbilities(EffectTarget.IterationEntity, Duration.EndOfTurn),
        ) then
            Effects.DestroyAll(GameObjectFilter.Creature.manaValueAtMostX())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Matteo Bassini"
        flavorText = "The solar eclipse robbed Firebenders of their ability to firebend, giving " +
            "Team Avatar an eight-minute window to storm the capital and defeat the Fire Lord."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0e24797-3e45-4c2b-b4b0-1ef44d42eaee.jpg?1764120649"
    }
}
