package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Suffocating Fumes — Ikoria: Lair of Behemoths #100
 * {2}{B} · Instant
 *
 * Creatures your opponents control get -1/-1 until end of turn.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 *
 * The mass debuff is a resolution-time [Effects.ForEachInGroup] over the opponents' creatures,
 * applying [Effects.ModifyStats] to each — creatures entering later are unaffected.
 */
val SuffocatingFumes = card("Suffocating Fumes") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -1/-1 until end of turn.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.opponentControls()),
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity)
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"Yesterday it was flying tigers and crumbling bridges. Who knows what today will bring?\"\n" +
            "—Dol, Lavabrink quartermaster"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66b562e4-35df-4aee-848d-ceb4204bbe58.jpg"
    }
}
