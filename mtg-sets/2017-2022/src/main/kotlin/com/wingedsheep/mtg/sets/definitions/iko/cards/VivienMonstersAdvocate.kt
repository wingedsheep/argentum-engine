package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vivien, Monsters' Advocate — Ikoria: Lair of Behemoths #175 (canonical printing)
 * {3}{G}{G} · Legendary Planeswalker — Vivien · Loyalty 3
 *
 * - Statics: Elven Chorus's pair — [LookAtTopOfLibrary] + [CastSpellTypesFromTopOfLibrary] over
 *   creature spells.
 * - +1: Invoke the Ancients' token-then-choose shape — the counter choice loops over
 *   `CollectionSlot.CreatedTokens`, so a token doubler yields one choice per token.
 * - −2: Summon: Fenrir's one-shot, end-of-turn "when you next cast a creature spell" delayed
 *   trigger, paying off with Rushed Rebirth's strict "lesser mana value" search
 *   (`triggeringManaValue() - 1`). The triggering spell is still on the stack when the trigger
 *   resolves, so its mana value includes the announced X (ruling).
 */
val VivienMonstersAdvocate = card("Vivien, Monsters' Advocate") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Vivien"
    startingLoyalty = 3
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may cast creature spells from the top of your library.\n" +
        "+1: Create a 3/3 green Beast creature token. Put your choice of a reach counter, a vigilance " +
        "counter, or a trample counter on it.\n" +
        "−2: When you next cast a creature spell this turn, search your library for a creature card " +
        "with lesser mana value, put it onto the battlefield, then shuffle."

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(filter = GameObjectFilter.Creature)
    }

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/1/f/1f2c9241-9cba-4c99-85cc-7e74286d1dc1.jpg?1783930944",
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        label = "Reach counter",
                        effect = Effects.AddCounters(CounterType.REACH, 1, EffectTarget.IterationEntity),
                    ),
                    EffectChoice(
                        label = "Vigilance counter",
                        effect = Effects.AddCounters(CounterType.VIGILANCE, 1, EffectTarget.IterationEntity),
                    ),
                    EffectChoice(
                        label = "Trample counter",
                        effect = Effects.AddCounters(CounterType.TRAMPLE, 1, EffectTarget.IterationEntity),
                    ),
                )
            ),
        )
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.you.casts(GameObjectFilter.Creature),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature.manaValueAtMostDynamic(
                    DynamicAmounts.triggeringManaValue() - 1
                ),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                shuffleAfter = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "175"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/504ebb84-7e7b-4119-a128-a9c183c5d9de.jpg?1783931029"
        ruling("2020-04-17", "Vivien lets you look at the top card of your library whenever you want (with one restriction; see below), even if you don't have priority. This action doesn't use the stack. Knowing what that card is becomes part of the information you have access to, just like you can look at the cards in your hand.")
        ruling("2020-04-17", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, you can't look at the new top card until you finish doing so. This means that if you cast the top card of your library, you can't look at the next one until you're done paying for that spell.")
        ruling("2020-04-17", "Normally, Vivien allows you to cast the top card of your library if it's a creature card, it's your main phase, and the stack is empty. If that creature card has flash, you'll be able to cast it any time you could cast an instant, even on an opponent's turn.")
        ruling("2020-04-17", "You'll still pay all costs for that spell, including additional costs. You may also pay alternative costs such as mutate costs.")
        ruling("2020-04-17", "The top card of your library isn't in your hand, so you can't cycle it, discard it, or activate any of its activated abilities.")
        ruling("2020-04-17", "After activating Vivien's last loyalty ability, the delayed triggered ability it creates will put a creature card onto the battlefield before the creature spell that caused the ability to trigger resolves.")
        ruling("2020-04-17", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value.")
        ruling("2020-04-17", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "If you cast a mutating creature spell, you must choose a target for it before the triggered ability from Vivien's last ability puts a creature onto the battlefield. The creature you find can't be the target of the mutating creature spell.")
    }
}
