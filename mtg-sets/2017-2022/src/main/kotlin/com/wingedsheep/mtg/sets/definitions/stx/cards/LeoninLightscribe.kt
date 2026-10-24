package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leonin Lightscribe — Strixhaven: School of Mages #20 (canonical printing)
 * {1}{W} · Creature — Cat Cleric · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, creatures you control get
 * +1/+1 until end of turn.
 *
 * Magecraft is [Triggers.you] `.castsOrCopies(InstantOrSorcery)`; the team pump is
 * [Effects.ForEachInGroup] over the creatures you control at resolution.
 */
val LeoninLightscribe = card("Leonin Lightscribe") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Cleric"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, creatures you " +
        "control get +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, creatures " +
            "you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Steven Belledin"
        flavorText = "All who hear his inspiring words are filled with hope and pride."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a465d7b-398c-444b-9eae-331ea2513f6d.jpg?1783927388"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
    }
}
