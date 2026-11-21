package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nivix Cyclops
 * {1}{U}{R}
 * Creature — Cyclops
 * 1/4
 * Defender
 * Whenever you cast an instant or sorcery spell, this creature gets +3/+0 until end of turn and can
 * attack this turn as though it didn't have defender.
 *
 * Canonical printing: Dragon's Maze (its earliest printing; later DDS / PIO printings are reprints).
 */
val NivixCyclops = card("Nivix Cyclops") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Cyclops"
    power = 1
    toughness = 4
    oracleText = "Defender\nWhenever you cast an instant or sorcery spell, this creature gets +3/+0 " +
        "until end of turn and can attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(3, 0, EffectTarget.Self) then
            Effects.CanAttackDespiteDefenderThisTurn()
        description = "Whenever you cast an instant or sorcery spell, this creature gets +3/+0 until " +
            "end of turn and can attack this turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d7a0e26-8cd4-4f53-8922-93ca28b1879b.jpg?1783940025"
        ruling("2013-04-15", "Nivix Cyclops still has defender after its triggered ability resolves, although it will be able to attack.")
    }
}
