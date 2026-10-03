package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Expel the Unworthy — Modern Horizons 3 #25
 * {1}{W} · Sorcery
 *
 * Kicker {2}{W}
 * Choose target creature with mana value 3 or less. If this spell was kicked, instead choose
 * target creature. Exile the chosen creature, then its controller gains life equal to its mana
 * value.
 *
 * "Instead" swaps the target restriction, so the kicked cast announces a different target
 * (`kickerTarget` / `kickerEffect`, as Cruel Alliance and Fight with Fire do). Both branches run
 * the same exile + life gain.
 *
 * The life gain is sequenced *before* the exile — the Swords to Plowshares / Crumble house
 * pattern — so the creature's controller ([EffectTarget.TargetController]) and mana value are
 * read while it is still on the battlefield; once exiled, the card's controller would fall back
 * to its owner. The life gained is identical under either order.
 */
val ExpelTheUnworthy = card("Expel the Unworthy") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Kicker {2}{W} (You may pay an additional {2}{W} as you cast this spell.)\n" +
        "Choose target creature with mana value 3 or less. If this spell was kicked, instead " +
        "choose target creature. Exile the chosen creature, then its controller gains life equal " +
        "to its mana value."

    keywordAbility(KeywordAbility.kicker("{2}{W}"))

    spell {
        val small = target(TargetFilter.Creature.manaValueAtMost(3))
        effect = Effects.GainLife(DynamicAmounts.manaValueOf(small), EffectTarget.TargetController) then
            Effects.Exile(small)

        val anyCreature = kickerTarget(TargetFilter.Creature)
        kickerEffect = Effects.GainLife(DynamicAmounts.manaValueOf(anyCreature), EffectTarget.TargetController) then
            Effects.Exile(anyCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Danny Schwartz"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f68baee-f503-407d-931f-2a550470a55f.jpg?1783911302"
        ruling("2024-06-07", "Use the creature's mana value as it last existed on the battlefield to determine how much life its controller gains.")
    }
}
