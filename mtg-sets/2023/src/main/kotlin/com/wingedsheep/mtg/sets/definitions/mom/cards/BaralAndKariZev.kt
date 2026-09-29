package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baral and Kari Zev
 * {1}{U}{R}
 * Legendary Creature — Human
 * 2/4
 *
 * First strike, menace
 * Whenever you cast your first instant or sorcery spell each turn, you may cast a spell with lesser
 * mana value that shares a card type with it from your hand without paying its mana cost. If you
 * don't, create First Mate Ragavan, a legendary 2/1 red Monkey Pirate creature token. It gains
 * haste until end of turn.
 *
 * - "Your first instant or sorcery spell each turn" narrows the trigger event itself, so it is a
 *   `triggerRestriction` (Aquatic Alchemist's shape), not an intervening "if" re-checked on
 *   resolution — casting a second instant in response doesn't stop the trigger.
 * - The payoff is Kellan, the Kid's `IfYouDo`: gather hand cards with mana value at most the
 *   triggering spell's mana value minus one ("lesser") that share a card type with the triggering
 *   spell, choose up to one, cast it for free. "If you don't" (nothing cast) creates the legendary
 *   Ragavan token and grants it haste until end of turn.
 */
val BaralAndKariZev = card("Baral and Kari Zev") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human"
    power = 2
    toughness = 4
    oracleText = "First strike, menace\n" +
        "Whenever you cast your first instant or sorcery spell each turn, you may cast a spell with " +
        "lesser mana value that shares a card type with it from your hand without paying its mana " +
        "cost. If you don't, create First Mate Ragavan, a legendary 2/1 red Monkey Pirate creature " +
        "token. It gains haste until end of turn."

    keywords(Keyword.FIRST_STRIKE, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.casts()
        triggerRestriction = Conditions.YouCastFirstSpellOfTypeThisTurn(GameObjectFilter.InstantOrSorcery)
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val inHand = gather(CardSource.FromZone(Zone.HAND))
                val eligible = filter(
                    inHand,
                    GameObjectFilter.Any
                        .manaValueAtMostDynamic(DynamicAmounts.triggeringSpellManaValue() - 1)
                        .sharingCardTypeWith(EffectTarget.TriggeringEntity)
                )
                val chosen = chooseUpTo(
                    1,
                    from = eligible,
                    selectedLabel = "Cast without paying its mana cost"
                )
                run(Effects.CastFromCollectionWithoutPayingCost(chosen, storeCastTo = "baralCast"))
            },
            then = Effects.Nothing,
            otherwise = Effects.CreateToken(
                power = 2,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Monkey", "Pirate"),
                name = "First Mate Ragavan",
                legendary = true,
                imageUri = "https://cards.scryfall.io/normal/front/f/6/f69504a4-8caf-40c7-b998-1558a00444fc.jpg?1783916671"
            ) then Effects.ForEachInCollection(
                CollectionSlot.CreatedTokens,
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
            ),
            successCriterion = SuccessCriterion.CollectionNonEmpty("baralCast"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc43a788-ab06-4d28-b45a-aa47801d6ace.jpg?1783916956"
        ruling("2023-04-14", "If the spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2023-04-14", "Baral and Kari Zev will count any instant or sorcery spell you cast during a turn, even if it wasn't on the battlefield at the time. This means if you cast an instant or sorcery spell, then Baral and Kari Zev comes under your control, its triggered ability won't trigger that turn.")
        ruling("2023-04-14", "The spell you cast from your hand without paying its mana cost is cast during the resolution of the triggered ability. Timing restrictions of that spell based on card type are ignored. It will resolve before the spell that caused the ability to trigger.")
        ruling("2023-04-14", "If you cast a spell without paying its mana cost, you can't choose to cast it for any alternative costs. You can, however, pay any additional costs. If the spell has any mandatory additional costs, you must pay those.")
    }
}
