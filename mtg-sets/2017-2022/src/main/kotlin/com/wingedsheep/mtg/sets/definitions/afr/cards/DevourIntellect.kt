package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Devour Intellect
 * {B}
 * Sorcery
 * Target opponent discards a card. If mana from a Treasure was spent to cast this spell, instead
 * that player reveals their hand, you choose a nonland card from it, then that player discards
 * that card.
 *
 * The Treasure check reads the spell's recorded mana provenance
 * ([DynamicAmounts.manaSpentFromSubtype]) as it resolves — merely controlling a Treasure doesn't
 * count, and a Treasure sacrificed for its own mana still does. Without Treasure mana the opponent
 * picks the discard; with it, the caster picks a nonland card from the revealed hand (Duress shape).
 */
val DevourIntellect = card("Devour Intellect") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards a card. If mana from a Treasure was spent to cast this spell, " +
        "instead that player reveals their hand, you choose a nonland card from it, then that player " +
        "discards that card."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE),
                ComparisonOperator.GTE,
                1,
            ),
            then = Effects.Pipeline {
                run(Effects.RevealHand(opponent))
                val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                val toDiscard = chooseExactly(
                    1,
                    from = opponentHand,
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    prompt = "Choose a nonland card to discard",
                    alwaysPrompt = true,
                    showAllCards = true,
                )
                discard(toDiscard, opponent.asPlayer)
            },
            otherwise = Effects.Discard(1, opponent),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Steven Russell Black"
        flavorText = "\"I'll know what you know soon enough.\"\n—Nihiloor, Xanathar Guild spymaster"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13c34f2d-2eac-4241-8b23-9cab3268c254.jpg?1783926498"
    }
}
