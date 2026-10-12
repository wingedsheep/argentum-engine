package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ajani, Sleeper Agent — Dominaria United #192
 * {1}{G}{G/W/P}{W} · Legendary Planeswalker — Ajani · Starting loyalty 4
 *
 * The −3's division is announced as the ability is activated (CR 601.2d): the legal action flags
 * it and the stack object carries the chosen split, so a creature removed in response loses its
 * share instead of the counters being re-divided. Every target, counters or not, gains vigilance.
 * The −6 emblem is a global triggered ability, so it outlives Ajani.
 */
val AjaniSleeperAgent = card("Ajani, Sleeper Agent") {
    manaCost = "{1}{G}{G/W/P}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 4
    oracleText = "Compleated ({G/W/P} can be paid with {G}, {W}, or 2 life. If life was paid, this planeswalker enters with two fewer loyalty counters.)\n" +
        "+1: Reveal the top card of your library. If it's a creature or planeswalker card, put it into your hand. Otherwise, you may put it on the bottom of your library.\n" +
        "−3: Distribute three +1/+1 counters among up to three target creatures. They gain vigilance until end of turn.\n" +
        "−6: You get an emblem with \"Whenever you cast a creature or planeswalker spell, target opponent gets two poison counters.\""

    keywords(Keyword.COMPLEATED)

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1))
            reveal(revealed)
            val (hits, misses) = filterSplit(revealed, GameObjectFilter.CreatureOrPlaneswalker)
            toHand(hits)
            val toBottom = chooseUpTo(
                1,
                from = misses,
                selectedLabel = "Put on bottom",
                remainderLabel = "Leave on top"
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
    }

    loyaltyAbility(-3) {
        target = TargetObject(filter = TargetFilter.Creature, count = 3, minCount = 0)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 3) then
            Effects.ForEachTarget(Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.ContextTarget(0)))
    }

    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.CreatureOrPlaneswalker)
                val opponent = target(Targets.Opponent)
                effect = Effects.AddCounters(CounterType.POISON, 2, opponent)
                description = "Whenever you cast a creature or planeswalker spell, target opponent gets two poison counters."
            },
            descriptionOverride = "Whenever you cast a creature or planeswalker spell, target opponent gets two poison counters."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7641f4d9-4614-41c8-87f5-4845bd78e9b3.jpg?1783921289"
        ruling("2022-09-09", "A hybrid Phyrexian mana symbol is counted the same way as a normal mana symbol when determining the mana value of a card. Specifically, Ajani’s mana value is always 4, even if you pay {1}{G}{W} and 2 life to cast him.")
        ruling("2022-09-09", "Phyrexian is not a color nor a type of mana, and players cannot add Phyrexian mana. It’s just a symbol that gives you another way to pay for a spell or ability.")
        ruling("2022-09-09", "Compleated is a replacement effect, and it applies only to a permanent that is entering the battlefield with loyalty counters. Any other replacement effect that would apply to the number of loyalty counters it enters the battlefield with will apply as normal.")
        ruling("2022-09-09", "The compleated ability looks only at whether a player chose to pay 2 life for a Phyrexian mana symbol as they were casting the spell. If a player paid life for some other reason while casting the spell, that will not reduce the number of loyalty counters the planeswalker enters the battlefield with.")
    }
}
