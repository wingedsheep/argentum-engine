package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sorin of House Markov // Sorin, Ravenous Neonate — Modern Horizons 3 #245
 * {1}{B} · Legendary Creature — Human Noble 1/4 // Legendary Planeswalker — Sorin (loyalty 3)
 *
 * Front:
 *   Lifelink
 *   Extort
 *   At the beginning of each of your postcombat main phases, if you gained 3 or more life this
 *   turn, exile Sorin, then return him to the battlefield transformed under his owner's control.
 * Back:
 *   Extort
 *   +2: Create a Food token.
 *   −1: Sorin deals damage equal to the amount of life you gained this turn to any target.
 *   −6: Gain control of target creature. It becomes a Vampire in addition to its other types. Put a
 *       lifelink counter on it if you control a white permanent other than that creature or Sorin.
 *
 * Modeling notes:
 *  - **Extort** is composed exactly as on The Kingpin of Crime: `Triggers.you.casts()` +
 *    [Effects.MayPay] `{W/B}` over [Effects.DrainLife] (one event, so the gain is the total lost).
 *  - The transform trigger is an intervening "if" ([Conditions.YouGainedLifeThisTurnAtLeast]),
 *    checked as the phase begins and again on resolution (CR 603.4), into
 *    [Effects.ExileAndReturnTransformed] — a new object returns back face up with loyalty 3.
 *  - −6's control change and the Vampire type are both permanent (ruling: the control effect lasts
 *    indefinitely). "A white permanent other than that creature or Sorin": count the white
 *    permanents you control other than Sorin; once you control the stolen creature it is among them
 *    if it is white, so a white target needs two, any other target needs one.
 */
private val whitePermanent = GameObjectFilter.Permanent.withColor(Color.WHITE)

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

private val SorinOfHouseMarkovFront = card("Sorin of House Markov") {
    manaCost = "{1}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Noble"
    power = 1
    toughness = 4
    oracleText = "Lifelink\n" +
        "$EXTORT_TEXT\n" +
        "At the beginning of each of your postcombat main phases, if you gained 3 or more life this " +
        "turn, exile Sorin, then return him to the battlefield transformed under his owner's control."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = Effects.ExileAndReturnTransformed(EffectTarget.Self)
        description = "At the beginning of each of your postcombat main phases, if you gained 3 or " +
            "more life this turn, exile Sorin, then return him to the battlefield transformed under " +
            "his owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d7474fc-0042-4be9-81f3-5f66f4b16740.jpg?1783911233"

        ruling("2024-06-07", "In some rare cases, a spell or ability may cause Sorin of House Markov to transform while he's a creature (front face up) on the battlefield. If this happens, Sorin, Ravenous Neonate won't have any loyalty counters on him and will subsequently be put into his owner's graveyard.")
        ruling("2024-06-07", "Sorin of House Markov doesn't need to have been on the battlefield when you gained the life. For example, if you gained 3 or more life during your upkeep and you cast Sorin of House Markov during your first main phase, its last ability will trigger at the beginning of your postcombat main phase.")
        ruling("2024-06-07", "Sorin of House Markov's last ability will trigger only once during your postcombat main phase, no matter how much life you gained this turn. However, if you haven't gained life so far this turn as your postcombat main phase begins, the ability won't trigger at all. It's not possible to gain life during your postcombat main phase in time for the ability to trigger.")
        ruling("2024-06-07", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-06-07", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
    }
}

private val SorinRavenousNeonate = card("Sorin, Ravenous Neonate") {
    manaCost = ""
    colorIdentity = "WB"
    colorIndicator = "WB"
    typeLine = "Legendary Planeswalker — Sorin"
    startingLoyalty = 3
    oracleText = "$EXTORT_TEXT\n" +
        "+2: Create a Food token.\n" +
        "−1: Sorin deals damage equal to the amount of life you gained this turn to any target.\n" +
        "−6: Gain control of target creature. It becomes a Vampire in addition to its other types. " +
        "Put a lifelink counter on it if you control a white permanent other than that creature or Sorin."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    loyaltyAbility(+2) {
        effect = Effects.CreateFood()
        description = "Create a Food token."
    }

    loyaltyAbility(-1) {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.lifeGainedThisTurn(), t)
        description = "Sorin deals damage equal to the amount of life you gained this turn to any target."
    }

    loyaltyAbility(-6) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature) then
            Effects.AddCreatureType("Vampire", creature) then
            Effects.If(
                Conditions.Any(
                    Conditions.YouControlOtherAtLeast(2, whitePermanent),
                    Conditions.All(
                        Conditions.YouControlOtherAtLeast(1, whitePermanent),
                        Conditions.Not(Conditions.TargetMatchesFilter(whitePermanent.youControl(), creature)),
                    ),
                ),
                Effects.AddCounters(CounterType.LIFELINK, 1, creature),
            )
        description = "Gain control of target creature. It becomes a Vampire in addition to its other " +
            "types. Put a lifelink counter on it if you control a white permanent other than that " +
            "creature or Sorin."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/back/1/d/1d7474fc-0042-4be9-81f3-5f66f4b16740.jpg?1783911233"

        ruling("2024-06-07", "You can activate one of Sorin, Ravenous Neonate's loyalty abilities the turn he enters the battlefield. However, you may do so only during one of your main phases when the stack is empty.")
        ruling("2024-06-07", "Sorin, Ravenous Neonate's second loyalty ability counts the total amount of life you gained without taking into account any life you lost during that turn. For example, if you gained 3 life and lost 3 life earlier in the turn, Sorin, Ravenous Neonate will deal 3 damage to the target.")
        ruling("2024-06-07", "The control effect created by Sorin, Ravenous Neonate's last ability lasts indefinitely. It doesn't wear off during the cleanup step or if Sorin, Ravenous Neonate leaves the battlefield.")
    }
}

val SorinOfHouseMarkov: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SorinOfHouseMarkovFront,
    backFace = SorinRavenousNeonate,
)
