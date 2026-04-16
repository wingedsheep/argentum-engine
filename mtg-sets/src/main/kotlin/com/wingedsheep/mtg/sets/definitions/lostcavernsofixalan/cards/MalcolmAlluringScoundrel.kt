package com.wingedsheep.mtg.sets.definitions.lostcavernsofixalan.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastTargetFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Malcolm, Alluring Scoundrel {1}{U}
 * Legendary Creature — Siren Pirate
 * 2/1
 *
 * Flash
 * Flying
 * Whenever Malcolm, Alluring Scoundrel deals combat damage to a player, put a chorus
 * counter on it. Draw a card, then discard a card. If there are four or more chorus
 * counters on Malcolm, Alluring Scoundrel, you may cast the discarded card without
 * paying its mana cost.
 *
 * Implementation notes:
 *  - The "cast the discarded card without paying its mana cost" clause reuses the
 *    exile-based free-cast pipeline (same pattern as Wishing Well / Portent of
 *    Calamity). After the card is discarded to the graveyard, if the counter
 *    threshold is met we move it from graveyard to exile and grant a one-shot
 *    free cast via `GrantFreeCastTargetFromExileEffect`.
 *  - If the controller declines the free cast, the card remains in exile rather
 *    than returning to the graveyard. This is a minor deviation from oracle text
 *    but matches how the engine models comparable "may cast" windows for other
 *    cards (see PortentOfCalamity and WishingWell).
 */
val MalcolmAlluringScoundrel = card("Malcolm, Alluring Scoundrel") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Creature — Siren Pirate"
    power = 2
    toughness = 1
    oracleText = "Flash\nFlying\nWhenever Malcolm, Alluring Scoundrel deals combat damage to a player, put a chorus counter on it. Draw a card, then discard a card. If there are four or more chorus counters on Malcolm, Alluring Scoundrel, you may cast the discarded card without paying its mana cost."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = CompositeEffect(
            listOf(
                // Put a chorus counter on Malcolm.
                Effects.AddCounters(Counters.CHORUS, 1, EffectTarget.Self),
                // Draw a card.
                Effects.DrawCards(1, EffectTarget.Controller),
                // Discard a card: gather hand → select one → move to graveyard.
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "discarded",
                    prompt = "Choose a card to discard"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                    moveType = MoveType.Discard
                ),
                // If Malcolm has four or more chorus counters, the controller may cast
                // the discarded card for free. Move it to exile and grant the permission.
                ConditionalEffect(
                    condition = Compare(
                        DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.CHORUS)),
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(4)
                    ),
                    effect = CompositeEffect(
                        listOf(
                            MoveCollectionEffect(
                                from = "discarded",
                                destination = CardDestination.ToZone(Zone.EXILE, Player.You)
                            ),
                            GrantFreeCastTargetFromExileEffect(
                                target = EffectTarget.PipelineTarget("discarded", 0)
                            )
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Fesbra"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19d6834d-afa3-4747-a62d-0654f4d9729f.jpg?1699043813"

        ruling("2023-11-10", "If you cast a spell using Malcolm, Alluring Scoundrel's last ability, you do so as part of the resolution of the ability. You can't wait to cast the spell later in the turn. Timing permissions based on the card's type are ignored.")
        ruling("2023-11-10", "You may not play land cards discarded with Malcolm, Alluring Scoundrel's last ability.")
        ruling("2023-11-10", "If you cast a card \"without paying its mana cost\", you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, you must pay those to cast the card.")
        ruling("2023-11-10", "If the spell has {X} in its mana cost, you must choose 0 as the value of X.")
        ruling("2023-11-10", "If Malcolm, Alluring Scoundrel isn't on the battlefield as its triggered ability resolves, you won't put a chorus counter on it, but you'll still draw a card and discard a card. You may still cast the discarded card without paying its mana cost if Malcolm had four or more chorus counters on it when it was last on the battlefield.")
    }
}
