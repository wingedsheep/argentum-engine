package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Struggle for Skemfar — Kaldheim #196
 * {3}{G} · Sorcery
 *
 * Put a +1/+1 counter on target creature you control. Then that creature fights up to one target
 * creature you don't control.
 * Foretell {G}
 *
 * The counter lands first, so the fight uses the boosted power. With no second target chosen the
 * creature still gets its counter and fights nobody (Arni Slays the Troll's chapter I shape).
 */
val StruggleForSkemfar = card("Struggle for Skemfar") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control. Then that creature fights up to " +
        "one target creature you don't control. (Each deals damage equal to its power to the other.)\n" +
        "Foretell {G} (During your turn, you may pay {2} and exile this card from your hand face down. " +
        "Cast it on a later turn for its foretell cost.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, mine) then
            Effects.Fight(mine, theirs)
    }

    keywordAbility(KeywordAbility.foretell("{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a67747a9-3299-4b4a-a4e0-e8a269c4f927.jpg?1783928203"
        ruling("2021-02-05", "You can cast Struggle for Skemfar targeting only a creature you control.")
        ruling(
            "2021-02-05",
            "If you choose two target creatures and either target is an illegal target as Struggle for " +
                "Skemfar tries to resolve, neither creature will deal or be dealt damage."
        )
        ruling(
            "2021-02-05",
            "If the creature you control is an illegal target as Struggle for Skemfar tries to resolve, " +
                "you won't put a +1/+1 counter on it. If that creature is a legal target but the other " +
                "creature isn't, you'll still put the +1/+1 counter on the creature you control."
        )
    }
}
