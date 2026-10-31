package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra, Acolyte of Flame
 * {1}{R}{R}
 * Legendary Planeswalker — Chandra
 * Loyalty: 4
 *
 * 0: Put a loyalty counter on each red planeswalker you control.
 * 0: Create two 1/1 red Elemental creature tokens. They gain haste. Sacrifice them at the
 *    beginning of the next end step.
 * −2: You may cast target instant or sorcery card with mana value 3 or less from your graveyard.
 *     If that spell would be put into your graveyard, exile it instead.
 *
 * "They gain haste" is an effect granted to the created tokens (no duration), not a keyword
 * printed on the token, so it is applied over the created-tokens collection. The −2 casts during
 * resolution, paying the spell's costs (Chandra, Flame's Catalyst's shape).
 */
val ChandraAcolyteOfFlame = card("Chandra, Acolyte of Flame") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "0: Put a loyalty counter on each red planeswalker you control.\n" +
        "0: Create two 1/1 red Elemental creature tokens. They gain haste. Sacrifice them at the " +
        "beginning of the next end step.\n" +
        "−2: You may cast target instant or sorcery card with mana value 3 or less from your " +
        "graveyard. If that spell would be put into your graveyard, exile it instead."

    loyaltyAbility(0) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Planeswalker.withColor(Color.RED).youControl()),
            Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.IterationEntity),
        )
        description = "Put a loyalty counter on each red planeswalker you control."
    }

    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            sacrificeAtStep = Step.END,
            imageUri = "https://cards.scryfall.io/normal/front/0/5/0574973b-a0d1-4e17-9ed7-42a98d25bb8d.jpg?1783932855",
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent),
        )
        description = "Create two 1/1 red Elemental creature tokens. They gain haste. Sacrifice them " +
            "at the beginning of the next end step."
    }

    loyaltyAbility(-2) {
        target(
            TargetFilter(
                GameObjectFilter.InstantOrSorcery.manaValueAtMost(3).ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Pipeline {
            val card = gather(CardSource.ChosenTargets)
            run(Effects.May(
                Effects.CastFromCollection(card, insteadOfGraveyard = AfterResolveDestination.EXILE),
                descriptionOverride = "You may cast target instant or sorcery card with mana value 3 or " +
                    "less from your graveyard.",
            ))
        }
        description = "You may cast target instant or sorcery card with mana value 3 or less from your " +
            "graveyard. If that spell would be put into your graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cabfcfe-012e-4cab-bba2-24052eac0946.jpg?1783932984"
        ruling("2019-07-12", "Chandra's first ability will put a counter on herself as well as any other red planeswalkers you control. This doesn't cause any other planeswalkers' loyalty abilities to be activated.")
        ruling("2019-07-12", "You must pay the costs for the spell you wish to cast with Chandra's last ability. If that spell has any alternative costs, you may pay those.")
        ruling("2019-07-12", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0 while you target it with Chandra's last ability; however, once you begin to cast it, you choose a value for X. If this causes the mana value to become greater than 3, you can still cast the spell.")
        ruling("2019-07-12", "If you wish to cast the target card, you must cast it while Chandra's last ability is resolving. You can't cast it later in the turn.")
    }
}
