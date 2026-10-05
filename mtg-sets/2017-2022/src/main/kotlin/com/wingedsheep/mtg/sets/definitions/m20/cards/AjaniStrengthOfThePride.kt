package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani, Strength of the Pride
 * {2}{W}{W}
 * Legendary Planeswalker — Ajani
 * Starting Loyalty: 5
 *
 * +1: You gain life equal to the number of creatures you control plus the number of
 *     planeswalkers you control.
 * −2: Create a 2/2 white Cat Soldier creature token named Ajani's Pridemate with "Whenever you
 *     gain life, put a +1/+1 counter on this token."
 * 0: If you have at least 15 life more than your starting life total, exile Ajani and each
 *    artifact and creature your opponents control.
 *
 * The +1 is a *sum* of two counts, not one count over "creature or planeswalker": per the
 * ruling, a planeswalker that is also a creature is counted twice. Ajani counts himself — he is
 * still a planeswalker you control while the ability resolves.
 *
 * The 0 checks the life threshold on resolution ([Conditions.LifeAboveStartingBy], same shape as
 * Chalice of Life); the opposing artifacts and creatures are gathered before Ajani leaves, so the
 * controller-relative "your opponents" filter is read while the ability's source still exists.
 */
val AjaniStrengthOfThePride = card("Ajani, Strength of the Pride") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 5
    oracleText = "+1: You gain life equal to the number of creatures you control plus the number of planeswalkers you control.\n" +
        "−2: Create a 2/2 white Cat Soldier creature token named Ajani's Pridemate with \"Whenever you gain life, put a +1/+1 counter on this token.\"\n" +
        "0: If you have at least 15 life more than your starting life total, exile Ajani and each artifact and creature your opponents control."

    // +1: You gain life equal to the number of creatures you control plus the number of
    //     planeswalkers you control.
    loyaltyAbility(+1) {
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count() +
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Planeswalker).count()
        )
    }

    // −2: Create a 2/2 white Cat Soldier creature token named Ajani's Pridemate with
    //     "Whenever you gain life, put a +1/+1 counter on this token."
    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat", "Soldier"),
            name = "Ajani's Pridemate",
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.you.gainsLife(),
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/0/b0819e8e-fb7e-43c7-a7cf-d768f43193ac.jpg?1783932857"
        )
    }

    // 0: If you have at least 15 life more than your starting life total, exile Ajani and each
    //    artifact and creature your opponents control.
    loyaltyAbility(0) {
        effect = Effects.If(
            condition = Conditions.LifeAboveStartingBy(15),
            then = Effects.Pipeline {
                val theirs = gather((GameObjectFilter.Artifact or GameObjectFilter.Creature).opponentControls())
                run(Effects.Exile(EffectTarget.Self))
                exile(theirs)
            }
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79883468-a37c-4894-8d05-6a4d150b7d59.jpg?1783933034"
        ruling(
            "2019-07-12",
            "If a planeswalker you control is also a creature (most likely because it's Gideon), " +
                "it will be counted twice as Ajani's first ability resolves."
        )
        ruling(
            "2019-07-12",
            "The tokens created by Ajani's second ability are similar to the card Ajani's Pridemate, " +
                "but they have no mana cost and their mana value is 0."
        )
        ruling(
            "2019-07-12",
            "Whether you have at least 15 life more than your starting life total is determined as " +
                "Ajani's last ability resolves."
        )
    }
}
