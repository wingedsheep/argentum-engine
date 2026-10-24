package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Symmetry Sage — Strixhaven: School of Mages #56 (canonical printing)
 * {U} · Creature — Human Wizard · 0/2
 *
 * Flying
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, target creature you control
 * has base power 2 until end of turn.
 *
 * "Has base power 2" sets only base power (layer 7b) — [Effects.SetBasePower], the Singing Tree
 * shape — so pumps and counters still apply on top (ruling).
 */
val SymmetrySage = card("Symmetry Sage") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 2
    oracleText = "Flying\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, target creature you " +
        "control has base power 2 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.SetBasePower(creature, DynamicAmounts.fixed(2), Duration.EndOfTurn)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, target " +
            "creature you control has base power 2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Jehan Choo"
        flavorText = "\"By my calculations, I should be able to achieve true enlightenment before graduation.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e726fc7-36cf-405c-9b7c-d1e41cd6c68f.jpg?1783927373"
        ruling("2021-04-16", "Symmetry Sage’s triggered ability overwrites all previous effects that set the target creature’s power to a specific value. Other effects that set its power to specific values that start to apply after this ability resolves will overwrite this effect.")
        ruling("2021-04-16", "Any effects that modify a creature’s power without setting it to a specific value (i.e. ones that don’t affect base power) will apply after its base power is set, regardless of the order those effects were created. The same is true for counters that modify its power.")
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
    }
}
