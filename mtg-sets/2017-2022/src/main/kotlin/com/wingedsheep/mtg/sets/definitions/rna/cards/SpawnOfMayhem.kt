package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spawn of Mayhem
 * {2}{B}{B}
 * Creature — Demon
 * 4/4
 * Spectacle {1}{B}{B} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * Flying, trample
 * At the beginning of your upkeep, this creature deals 1 damage to each player. Then if you have 10 or
 * less life, put a +1/+1 counter on this creature.
 *
 * The life check is not an intervening "if": it is read on resolution, after the damage is dealt.
 */
val SpawnOfMayhem = card("Spawn of Mayhem") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 4
    toughness = 4
    oracleText = "Spectacle {1}{B}{B} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "Flying, trample\n" +
        "At the beginning of your upkeep, this creature deals 1 damage to each player. Then if you have 10 or " +
        "less life, put a +1/+1 counter on this creature."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{1}{B}{B}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.ForEachPlayer(Player.Each, Effects.DealDamage(1, EffectTarget.Controller)) then
            Effects.If(
                condition = Conditions.LifeAtMost(10),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "85"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8be01b8d-e0ef-44d1-ac97-39bb5a0e76be.jpg?1783933688"
    }
}
