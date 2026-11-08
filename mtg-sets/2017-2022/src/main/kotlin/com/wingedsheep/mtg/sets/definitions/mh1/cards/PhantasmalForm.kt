package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Phantasmal Form
 * {2}{U}
 * Instant
 * Until end of turn, up to two target creatures each have base power and toughness 3/3, gain
 * flying, and become blue Illusions in addition to their other colors and types.
 * Draw a card.
 *
 * "Up to two target creatures each …" is a multi-slot target, so the per-creature changes run
 * under [Effects.ForEachTarget] against `ContextTarget(0)`. The draw sits outside the loop: it
 * happens once, even with zero targets chosen; if every chosen target is illegal on resolution
 * the spell doesn't resolve and nothing is drawn.
 */
val PhantasmalForm = card("Phantasmal Form") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, up to two target creatures each have base power and toughness 3/3, " +
        "gain flying, and become blue Illusions in addition to their other colors and types.\n" +
        "Draw a card."

    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.SetBasePowerAndToughness(3, 3, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.AddColor(Color.BLUE, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.AddCreatureType("Illusion", EffectTarget.ContextTarget(0), Duration.EndOfTurn),
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Steve Argyle"
        flavorText = "Within each of us lurks the fathomless."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f106e515-511a-48c7-b7a7-3ca820950122.jpg?1783933140"

        ruling("2019-06-14", "Phantasmal Form overwrites all previous effects that set the target creatures' power and toughness to specific values. Other effects that set their power or toughness to specific values that start to apply after Phantasmal Form resolves will overwrite this effect.")
        ruling("2019-06-14", "Effects that modify the target creatures' power or toughness (such as the effects of Force of Virtue or Giant Growth) will apply no matter when they started to take effect. The same is true for counters that change the creatures' power or toughness (such as +1/+1 counters).")
        ruling("2019-06-14", "You may cast Phantasmal Form without choosing any target creatures. You'll just draw a card. However, if you choose any targets and all of those targets become illegal before the spell tries to resolve, the spell won't resolve and you won't draw a card.")
    }
}
