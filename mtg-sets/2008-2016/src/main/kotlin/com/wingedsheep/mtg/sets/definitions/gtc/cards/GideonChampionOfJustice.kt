package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gideon, Champion of Justice
 * {2}{W}{W}
 * Legendary Planeswalker — Gideon
 * Starting Loyalty: 4
 *
 * +1: Put a loyalty counter on Gideon for each creature target opponent controls.
 * 0: Until end of turn, Gideon becomes a Human Soldier creature with power and toughness each
 *    equal to the number of loyalty counters on him and gains indestructible. He's still a
 *    planeswalker. Prevent all damage that would be dealt to him this turn.
 * −15: Exile all other permanents.
 *
 * The +1 counts the target opponent's creatures on resolution; the loyalty counter paid as the
 * cost stays even if the ability fizzles.
 *
 * The 0 keeps the planeswalker type (no `removeTypes`) and only *sets creature subtypes*, so the
 * Gideon planeswalker subtype survives (CR 205.1b). Its P/T is evaluated once on resolution and
 * locked for the turn, per the ruling — later loyalty changes don't move it.
 *
 * The −15 gathers every permanent but Gideon and exiles them together.
 */
val GideonChampionOfJustice = card("Gideon, Champion of Justice") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Gideon"
    startingLoyalty = 4
    oracleText = "+1: Put a loyalty counter on Gideon for each creature target opponent controls.\n" +
        "0: Until end of turn, Gideon becomes a Human Soldier creature with power and toughness each " +
        "equal to the number of loyalty counters on him and gains indestructible. He's still a " +
        "planeswalker. Prevent all damage that would be dealt to him this turn.\n" +
        "−15: Exile all other permanents."

    // +1: Put a loyalty counter on Gideon for each creature target opponent controls.
    loyaltyAbility(+1) {
        target = Targets.Opponent
        effect = Effects.AddDynamicCounters(
            CounterType.LOYALTY,
            DynamicAmounts.battlefield(Player.TargetOpponent, GameObjectFilter.Creature).count(),
            EffectTarget.Self
        )
    }

    // 0: Until end of turn, Gideon becomes a Human Soldier creature with P/T each equal to the
    //    number of loyalty counters on him and gains indestructible. He's still a planeswalker.
    //    Prevent all damage that would be dealt to him this turn.
    loyaltyAbility(0) {
        val loyalty = DynamicAmounts.countersOnSelf(CounterType.LOYALTY)
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = loyalty,
            toughness = loyalty,
            keywords = setOf(Keyword.INDESTRUCTIBLE),
            creatureTypes = setOf("Human", "Soldier"),
            duration = Duration.EndOfTurn
        ) then Effects.PreventDamage(target = EffectTarget.Self)
    }

    // −15: Exile all other permanents.
    loyaltyAbility(-15) {
        effect = Effects.Pipeline {
            val others = gather(GameObjectFilter.Permanent, excludeSelf = true)
            exile(others)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "13"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93d0509a-a863-4d9c-b39f-625a8cc1a547.jpg?1783940143"
        ruling(
            "2013-07-01",
            "If damage that can't be prevented is dealt to Gideon, Champion of Justice after his second " +
                "ability has resolved, that damage will have all applicable results: specifically, the damage " +
                "is marked on Gideon (since he's a creature) and that damage causes that many loyalty counters " +
                "to be removed from him (since he's a planeswalker). Even though he has indestructible, if " +
                "Gideon, Champion of Justice has no loyalty counters on him, he's put into his owner's " +
                "graveyard as a state-based action."
        )
        ruling(
            "2013-01-24",
            "If the first ability doesn't resolve (perhaps because the target opponent is an illegal target " +
                "when the ability tries to resolve), you won't put any additional loyalty counters on Gideon, " +
                "although the loyalty counter you put on him to activate the ability will remain."
        )
        ruling(
            "2013-01-24",
            "Gideon, Champion of Justice's power and toughness are set to the number of loyalty counters on " +
                "him when his second ability resolves. They won't change later in the turn if the number of " +
                "loyalty counters on him changes."
        )
        ruling(
            "2013-01-24",
            "Gideon, Champion of Justice's second ability causes him to become a creature with the creature " +
                "types Human and Soldier. He remains a planeswalker with the planeswalker type Gideon. (He also " +
                "retains any other card types or subtypes he may have had.) Each subtype is correlated to the " +
                "proper card type: Gideon is just a planeswalker type (not a creature type), and Human and " +
                "Soldier are just creature types (not planeswalker types)."
        )
        ruling(
            "2013-01-24",
            "If Gideon, Champion of Justice becomes a creature the same turn he enters, you can't attack " +
                "with him or use any of his {T} abilities (if he gains any)."
        )
    }
}
