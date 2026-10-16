package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vault 12: The Necropolis — Fallout #51
 * {4}{B}{B} · Enchantment — Saga · Rare
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Each player gets three rad counters.
 * II — Create X 2/2 black Zombie Mutant creature tokens, where X is the total number of rad
 * counters among players.
 * III — Put two +1/+1 counters on each creature you control that's a Zombie or Mutant.
 *
 * Chapter I is Megaton's Fate's "each player gets rad counters" (`ForEachPlayer` rebinding
 * `Controller`). Chapter II reads `PlayerCounterCount(RAD, Player.Each)`, which sums across every
 * player when the chapter resolves. Chapter III's "Zombie or Mutant" is one `withAnySubtype`
 * match per creature, so a Zombie Mutant still gets only two counters.
 */
val Vault12TheNecropolis = card("Vault 12: The Necropolis") {
    manaCost = "{4}{B}{B}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Each player gets three rad counters.\n" +
        "II — Create X 2/2 black Zombie Mutant creature tokens, where X is the total number of rad " +
        "counters among players.\n" +
        "III — Put two +1/+1 counters on each creature you control that's a Zombie or Mutant."

    sagaChapter(1) {
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.AddCounters(CounterType.RAD, 3, EffectTarget.Controller)
        )
    }
    sagaChapter(2) {
        effect = Effects.CreateToken(
            count = DynamicAmounts.playerCounterCount(CounterType.RAD, Player.Each),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Mutant"),
            imageUri = "https://cards.scryfall.io/normal/front/b/a/ba45bc9c-8257-43f8-9ba1-116b9fea90f4.jpg?1783912046"
        )
    }
    sagaChapter(3) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withAnySubtype("Zombie", "Mutant").youControl()),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/144cd9eb-9b15-40ce-8a48-919e72dbaa67.jpg?1783912407"
        ruling(
            "2024-03-08",
            "Use the number of rad counters among all players still in the game when Vault 12's second " +
                "chapter ability resolves to determine the value of X."
        )
        ruling(
            "2024-03-08",
            "A creature you control that's both a Zombie and a Mutant will get only two +1/+1 counters " +
                "from Vault 12's last ability."
        )
    }
}
