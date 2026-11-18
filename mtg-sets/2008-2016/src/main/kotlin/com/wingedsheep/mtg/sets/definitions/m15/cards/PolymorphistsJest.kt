package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Polymorphist's Jest — Magic 2015 #75 (canonical printing)
 * {1}{U}{U} · Instant
 *
 * Until end of turn, each creature target player controls loses all abilities and becomes a blue
 * Frog with base power and toughness 1/1.
 *
 * The mass form of Turn to Frog. [Effects.ForEachInGroup] snapshots the target player's creatures
 * as the spell resolves, so creatures that enter or change control afterwards are untouched (the
 * card's own ruling), and each snapshotted creature gets Turn to Frog's four floating effects:
 * ability loss (layer 6), Frog as its only creature type (layer 4 — other card types and
 * supertypes stay), blue as its only colour (layer 5), and base 1/1 (layer 7b, so pumps and
 * counters still apply on top).
 */
val PolymorphistsJest = card("Polymorphist's Jest") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, each creature target player controls loses all abilities and becomes a blue Frog with base power and toughness 1/1."

    spell {
        val player = target(Targets.Player)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
            effect = Effects.RemoveAllAbilities(EffectTarget.IterationEntity, Duration.EndOfTurn) then
                Effects.SetCreatureSubtypes(setOf("Frog"), EffectTarget.IterationEntity, Duration.EndOfTurn) then
                Effects.ChangeColor(EffectTarget.IterationEntity, setOf(Color.BLUE), Duration.EndOfTurn) then
                Effects.SetBasePowerAndToughness(1, 1, EffectTarget.IterationEntity, Duration.EndOfTurn)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Craig J Spearing"
        flavorText = "\"The flies were bothering me.\"\n—Jalira, master polymorphist"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a83f369b-515a-43ba-8905-bc12e148beeb.jpg?1783939188"
        ruling("2014-07-18", "If one of the affected creatures gains an ability after Polymorphist's Jest resolves, it will keep that ability.")
        ruling("2014-07-18", "Creatures that enter the battlefield or come under the target player's control after Polymorphist's Jest resolves won't be affected.")
        ruling("2014-07-18", "The creatures will lose all other colors and creature types, but they will retain any other card types (such as artifact) or supertypes (such as legendary) they may have.")
        ruling("2014-07-18", "Polymorphist's Jest overwrites all previous effects that set the creatures' base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Polymorphist's Jest resolves will overwrite this effect.")
        ruling("2014-07-18", "Effects that modify a creature's power and/or toughness, such as the effect of Titanic Growth, will apply to the creatures no matter when they started to take effect. The same is true for any counters that change their power and/or toughness and effects that switch power and toughness.")
    }
}
