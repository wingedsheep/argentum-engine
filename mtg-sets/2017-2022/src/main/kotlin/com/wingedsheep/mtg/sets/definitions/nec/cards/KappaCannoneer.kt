package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kappa Cannoneer — Neon Dynasty Commander #14 (canonical printing)
 * {5}{U} · Artifact Creature — Turtle Warrior · 4/4
 *
 * Improvise
 * Ward {4}
 * Whenever this creature or another artifact you control enters, put a +1/+1 counter on this
 * creature. It can't be blocked this turn.
 *
 * The Cannoneer is itself an artifact, so "this creature or another artifact you control" is
 * exactly "an artifact you control" — one enters trigger over your artifacts covers both.
 */
val KappaCannoneer = card("Kappa Cannoneer") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Turtle Warrior"
    power = 4
    toughness = 4
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "Ward {4}\n" +
        "Whenever this creature or another artifact you control enters, put a +1/+1 counter on " +
        "this creature. It can't be blocked this turn."

    keywords(Keyword.IMPROVISE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{4}")))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85a89077-b384-4fca-9d26-7297962c1541.jpg?1783923994"
        ruling(
            "2022-02-18",
            "If a player casts a spell that targets multiple permanents their opponent controls " +
                "with ward, each of those ward abilities will trigger. If that player doesn't pay " +
                "for all of them, the spell will be countered."
        )
    }
}
