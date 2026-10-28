package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Myriad Construct — Zendikar Rising #246
 * {4} · Artifact Creature — Construct · 4/4
 *
 * Kicker {3}
 * If this creature was kicked, it enters with a +1/+1 counter on it for each nonbasic land your
 * opponents control.
 * When this creature becomes the target of a spell, sacrifice it and create a number of 1/1
 * colorless Construct artifact creature tokens equal to its power.
 *
 * - The kicked counters are an as-enters replacement (CR 614.1c), Vodalian Mindsinger's shape: a
 *   [EntersWithDynamicCounters] whose count is zero unless the spell was kicked.
 * - The target trigger sacrifices first, then counts its power: off the battlefield
 *   `sourcePower()` reads last-known information, so the tokens equal its power as it last existed
 *   on the battlefield — and a second trigger that finds it already gone still creates tokens
 *   (ruling 2020-09-25).
 */
val MyriadConstruct = card("Myriad Construct") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "Kicker {3}\n" +
        "If this creature was kicked, it enters with a +1/+1 counter on it for each nonbasic land " +
        "your opponents control.\n" +
        "When this creature becomes the target of a spell, sacrifice it and create a number of 1/1 " +
        "colorless Construct artifact creature tokens equal to its power."

    keywordAbility(KeywordAbility.kicker("{3}"))

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.conditional(
                Conditions.WasKicked,
                DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.NonbasicLand).count(),
                DynamicAmounts.fixed(0),
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(spellsOnly = true)
        effect = Effects.SacrificeTarget(EffectTarget.Self) then
            Effects.CreateToken(
                count = DynamicAmounts.sourcePower(),
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Construct"),
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/3/4/3462c167-e6cf-46f8-9b77-2a88815e3b82.jpg?1783929496",
            )
        description = "When this creature becomes the target of a spell, sacrifice it and create a number " +
            "of 1/1 colorless Construct artifact creature tokens equal to its power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db9b1de4-9aba-4145-804d-2072f258af3b.jpg?1783929315"
        ruling("2020-09-25", "If you don't sacrifice Myriad Construct as its last ability resolves (perhaps because its ability triggered more than once and you've already sacrificed it), you still create Construct tokens. The number of tokens you create is determined by Myriad Construct's power as it last existed on the battlefield.")
        ruling("2020-09-25", "If a spell targets Myriad Construct more than once, its last ability triggers only once.")
        ruling("2020-09-25", "An ability that triggers when a permanent becomes the target of a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
    }
}
