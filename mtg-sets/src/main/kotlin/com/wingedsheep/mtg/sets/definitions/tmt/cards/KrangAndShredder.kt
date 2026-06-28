package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

// Each opponent exiles from the top of their library until a nonland card, all linked to Krang &
// Shredder so the Disappear ability can cast them. Shared by the enter and attack triggers.
private fun krangExileEachOpponent(): Effect =
    Effects.ForEachPlayer(
        Player.EachOpponent,
        listOf(
            GatherUntilMatchEffect(
                filter = GameObjectFilter.Nonland,
                storeMatch = "krangMatch",
                storeRevealed = "krangRevealed"
            ),
            MoveCollectionEffect(
                from = "krangRevealed",
                destination = CardDestination.ToZone(Zone.EXILE),
                linkToSource = true
            )
        )
    )

/**
 * Krang & Shredder
 * {4}{U/B}{U/B}
 * Legendary Creature — Utrom Human Ninja
 * 6/7
 *
 * Whenever Krang & Shredder enter or attack, each opponent exiles cards from the top
 * of their library until they exile a nonland card.
 * Disappear — At the beginning of your end step, if a permanent left the battlefield
 * under your control this turn, you may cast a card exiled with Krang & Shredder
 * without paying its mana cost.
 */
val KrangAndShredder = card("Krang & Shredder") {
    manaCost = "{4}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Utrom Human Ninja"
    oracleText = "Whenever Krang & Shredder enter or attack, each opponent exiles cards from the top of their library until they exile a nonland card.\nDisappear — At the beginning of your end step, if a permanent left the battlefield under your control this turn, you may cast a card exiled with Krang & Shredder without paying its mana cost."
    power = 6
    toughness = 7

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = krangExileEachOpponent()
        description = "Whenever Krang & Shredder enter or attack, each opponent exiles cards from the top of their library until they exile a nonland card."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = krangExileEachOpponent()
        description = "Whenever Krang & Shredder enter or attack, each opponent exiles cards from the top of their library until they exile a nonland card."
    }

    // Disappear: cast ONE of the linked-exiled cards for free at your end step. Gather the whole
    // linked-exile pile, let the player pick a single card, then cast just that one during
    // resolution — "you may cast a card" (singular), not the entire pile. The cast happens here
    // (cascade-style), not via a lingering "you may play it later" permission, so the card
    // actually enters the battlefield under the Disappear controller.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = MayEffect(
            Effects.Composite(
                listOf(
                    GatherCardsEffect(source = CardSource.FromLinkedExile(), storeAs = "krangCastable"),
                    // "you may CAST a card" — lands are played, not cast (CR 601/305), so the
                    // exiled lands (Krang exiles down *to* a nonland) can never be chosen here.
                    // Per the official ruling the chosen card is cast while this ability resolves,
                    // ignoring timing; only nonland cards are eligible to cast.
                    SelectFromCollectionEffect(
                        from = "krangCastable",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        filter = GameObjectFilter.Nonland,
                        storeSelected = "krangChosen",
                        prompt = "Choose a card exiled with Krang & Shredder to cast without paying its mana cost"
                    ),
                    CastFromCollectionWithoutPayingCostEffect(from = "krangChosen")
                )
            )
        )
        description = "Disappear — At the beginning of your end step, if a permanent left the battlefield under your control this turn, you may cast a card exiled with Krang & Shredder without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b5437e2-e3f4-4c19-a2aa-a75f65a001bb.jpg?1769006256"
    }
}
