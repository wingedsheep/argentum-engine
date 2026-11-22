package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragonshift — Dragon's Maze #66 (canonical printing)
 * {1}{U}{R} · Instant
 *
 * Until end of turn, target creature you control becomes a blue and red Dragon with base power and
 * toughness 4/4, loses all abilities, and gains flying.
 * Overload {3}{U}{U}{R}{R}
 *
 * Dance of the Skywise's shape: ability loss then a later flying grant (layer 6), Dragon as the only
 * creature type (layer 4 — other card types stay), blue and red as the only colours (layer 5), base
 * 4/4 (layer 7b). Overloaded, [Effects.ForEachInGroup] snapshots the creatures you control on
 * resolution, so creatures that come under your control later in the turn are untouched (ruling).
 */
private fun dragonshift(creature: EffectTarget): Effect =
    Effects.RemoveAllAbilities(creature, Duration.EndOfTurn) then
        Effects.SetCreatureSubtypes(setOf("Dragon"), creature, Duration.EndOfTurn) then
        Effects.ChangeColor(creature, setOf(Color.BLUE, Color.RED), Duration.EndOfTurn) then
        Effects.SetBasePowerAndToughness(4, 4, creature, Duration.EndOfTurn) then
        Effects.GrantKeyword(Keyword.FLYING, creature, Duration.EndOfTurn)

val Dragonshift = card("Dragonshift") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature you control becomes a blue and red Dragon with base " +
        "power and toughness 4/4, loses all abilities, and gains flying.\n" +
        "Overload {3}{U}{U}{R}{R} (You may cast this spell for its overload cost. If you do, change " +
        "\"target\" in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{3}{U}{U}{R}{R}"))

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = dragonshift(creature)

        overloadEffect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            effect = dragonshift(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c046e4e-810c-4123-bb1a-4f97e0cd43d1.jpg?1783940029"
        ruling("2024-01-12", "Because a spell with overload doesn't target when its overload cost is paid, it may affect permanents with hexproof or with protection from the appropriate color.")
        ruling("2013-04-15", "Dragonshift overwrites all previous effects that set a creature's power or toughness to specific values. However, effects that set a creature's power or toughness to specific values that start to apply after Dragonshift resolves will overwrite this effect.")
        ruling("2013-04-15", "If you cast Dragonshift using overload, only creatures you control when it resolves will be affected. Creatures that come under your control later in the turn will not.")
        ruling("2013-04-15", "Each affected creature will lose all other colors and creature types and be only red, blue, and a Dragon. Each will retain any other types it may have had, such as artifact.")
        ruling("2013-04-15", "Each affected creature will lose any abilities it may have gained prior to Dragonshift resolving. Notably, this includes the ability to cast the copies of a card with cipher that's encoded on the creature, although that ability will return after the turn ends.")
        ruling("2013-04-15", "If any of the affected creatures gains an ability after Dragonshift resolves, it will keep that ability.")
    }
}
