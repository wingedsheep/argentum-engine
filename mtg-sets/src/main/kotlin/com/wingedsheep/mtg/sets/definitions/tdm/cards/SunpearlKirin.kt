package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Sunpearl Kirin — Tarkir: Dragonstorm #29
 * {1}{W} · Creature — Kirin · 2/1
 *
 * Flash
 * Flying
 * When this creature enters, return up to one other target nonland permanent you control to its
 * owner's hand. If it was a token, draw a card.
 *
 * The ETB targets an optional ("up to one") other nonland permanent you control and returns it to
 * hand. The "if it was a token, draw a card" reflexive clause is modeled by capturing the chosen
 * target into a pipeline collection ([CardSource.ChosenTargets]) and gating an [Effects.DrawCards]
 * on [Conditions.CollectionContainsMatch] for [CardPredicate.IsToken]. The token check is evaluated
 * (and the draw resolved) before [Effects.ReturnToHand] removes the permanent, so the token's
 * [com.wingedsheep.engine.state.components.identity.TokenComponent] is still present when checked —
 * both happen in the same resolution, so the visible game state is identical to bouncing first.
 * Declining the optional target leaves the captured collection empty: no draw, no bounce.
 */
val SunpearlKirin = card("Sunpearl Kirin") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kirin"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "Flying\n" +
        "When this creature enters, return up to one other target nonland permanent you control to " +
        "its owner's hand. If it was a token, draw a card."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "other nonland permanent you control",
            TargetPermanent(
                optional = true,
                filter = TargetFilter.NonlandPermanent.youControl().other()
            )
        )
        effect = Effects.Composite(listOf(
            // Capture the chosen permanent so we can inspect its token status before it leaves.
            GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "returned"),
            // If the captured permanent was a token, draw a card (resolved while it's still in play).
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch(
                    "returned",
                    GameObjectFilter(cardPredicates = listOf(CardPredicate.IsToken))
                ),
                effect = Effects.DrawCards(1)
            ),
            // Return the chosen permanent to its owner's hand (no-op if no target was chosen).
            Effects.ReturnToHand(permanent)
        ))
        description = "When this creature enters, return up to one other target nonland permanent you " +
            "control to its owner's hand. If it was a token, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Allen Morris"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18292b9c-0f42-4ce2-8b85-35d06cf45a63.jpg?1743204071"
    }
}
