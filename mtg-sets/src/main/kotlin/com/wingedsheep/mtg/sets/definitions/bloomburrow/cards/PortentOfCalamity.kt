package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastTargetFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Portent of Calamity
 * {X}{U}
 * Sorcery
 *
 * Reveal the top X cards of your library. For each card type, you may exile a card
 * of that type from among them. Put the rest into your graveyard. You may cast a spell
 * from among the exiled cards without paying its mana cost if you exiled four or more
 * cards this way. Then put the rest of the exiled cards into your hand.
 *
 * Implementation:
 *  1. Reveal the top X cards into "revealed".
 *  2. Player chooses cards to exile — the engine enforces "at most one per card type"
 *     via [SelectionRestriction.OnePerCardType] layered over a normal ChooseUpTo mode.
 *  3. Unchosen cards go to the graveyard; chosen cards move to exile.
 *  4. If at least four cards were exiled, the player may pick one to cast for free from
 *     exile — that card stays in exile with a free-cast permission and the remaining
 *     exiled cards go to hand. Otherwise the entire exiled pile goes to hand.
 *
 *     Per the 2024-07-26 ruling, the free cast happens "as Portent of Calamity resolves".
 *     The engine models this by granting the free-cast permission immediately; the player
 *     can cast the marked card at their next priority window (in practice, right after
 *     Portent finishes resolving).
 */
val PortentOfCalamity = card("Portent of Calamity") {
    manaCost = "{X}{U}"
    typeLine = "Sorcery"
    oracleText = "Reveal the top X cards of your library. For each card type, you may exile a card of that type from among them. Put the rest into your graveyard. You may cast a spell from among the exiled cards without paying its mana cost if you exiled four or more cards this way. Then put the rest of the exiled cards into your hand."

    spell {
        effect = CompositeEffect(
            listOf(
                // 1. Reveal the top X cards.
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.XValue),
                    storeAs = "revealed",
                    revealed = true
                ),
                // 2. "For each card type, you may exile a card of that type from among them."
                //    The engine enforces the one-per-card-type constraint; the remainder
                //    flows into "graveyardPile" for the next step.
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(9)),
                    restrictions = listOf(SelectionRestriction.OnePerCardType),
                    storeSelected = "exiled",
                    storeRemainder = "graveyardPile",
                    prompt = "For each card type, you may exile a card of that type",
                    selectedLabel = "Exile",
                    remainderLabel = "Graveyard"
                ),
                // 3. Put the rest into your graveyard.
                MoveCollectionEffect(
                    from = "graveyardPile",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                ),
                // 4. Move the chosen cards to exile.
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                // 5a. If four or more cards were exiled: pick up to one to cast for free,
                //     grant the permission, then send the rest of the exiled cards to hand.
                // 5b. Otherwise: put all exiled cards into hand immediately.
                ConditionalOnCollectionEffect(
                    collection = "exiled",
                    minSize = 4,
                    ifNotEmpty = CompositeEffect(
                        listOf(
                            SelectFromCollectionEffect(
                                from = "exiled",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                                storeSelected = "freeCast",
                                storeRemainder = "toHand",
                                prompt = "You may cast a spell from among the exiled cards without paying its mana cost"
                            ),
                            GrantFreeCastTargetFromExileEffect(
                                target = EffectTarget.PipelineTarget("freeCast", 0)
                            ),
                            MoveCollectionEffect(
                                from = "toHand",
                                destination = CardDestination.ToZone(Zone.HAND)
                            )
                        )
                    ),
                    ifEmpty = MoveCollectionEffect(
                        from = "exiled",
                        destination = CardDestination.ToZone(Zone.HAND)
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Sam Guay"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/8599e2dd-9164-4da3-814f-adccef3b9497.jpg?1721426215"

        ruling("2024-07-26", "The card types that can appear on cards you reveal are artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery.")
        ruling("2024-07-26", "You choose which spell to cast (if any) as Portent of Calamity resolves. If you choose to cast a spell this way, you do so as part of the resolution of Portent of Calamity.")
        ruling("2024-07-26", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs.")
    }
}
