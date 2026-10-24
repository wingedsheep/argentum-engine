package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Witherbloom Command — Strixhaven: School of Mages #248 (canonical printing)
 * {B}{G} · Sorcery
 *
 * Choose two —
 * • Target player mills three cards, then you return a land card from your graveyard to your hand.
 * • Destroy target noncreature, nonland permanent with mana value 2 or less.
 * • Target creature gets -3/-1 until end of turn.
 * • Target opponent loses 2 life and you gain 2 life.
 *
 * A `modal(chooseCount = 2)` spell with per-mode targets. The mill mode is Corpse Churn's
 * pipeline, except the return is mandatory ("you return", not "you may") — `chooseExactly(1)`
 * over the land cards in your graveyard after the mill, so a land just milled (when you targeted
 * yourself) is eligible, and an empty graveyard selects nothing. The land isn't targeted.
 */
val WitherbloomCommand = card("Witherbloom Command") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText =
        "Choose two —\n" +
        "• Target player mills three cards, then you return a land card from your graveyard to your hand.\n" +
        "• Destroy target noncreature, nonland permanent with mana value 2 or less.\n" +
        "• Target creature gets -3/-1 until end of turn.\n" +
        "• Target opponent loses 2 life and you gain 2 life."

    spell {
        modal(chooseCount = 2) {
            mode("Target player mills three cards, then you return a land card from your graveyard to your hand") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    run(Patterns.Library.mill(3, player))
                    val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Land))
                    val chosen = chooseExactly(
                        1,
                        from = lands,
                        showAllCards = true,
                        prompt = "Return a land card from your graveyard to your hand",
                        selectedLabel = "Return to hand",
                        remainderLabel = "Leave in graveyard"
                    )
                    toHand(chosen)
                }
            }
            mode("Destroy target noncreature, nonland permanent with mana value 2 or less") {
                val permanent = target(
                    TargetFilter(GameObjectFilter.NonlandPermanent.notCreature().manaValueAtMost(2))
                )
                effect = Effects.Destroy(permanent)
            }
            mode("Target creature gets -3/-1 until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-3, -1, creature, Duration.EndOfTurn)
            }
            mode("Target opponent loses 2 life and you gain 2 life") {
                val opponent = target(Targets.Opponent)
                effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87d5e94b-0b35-4efd-9158-1767dcaea38c.jpg?1783927285"
        ruling("2021-04-16", "If you target yourself with the first mode, you may return any land card to your hand, including one you just milled or one that was already there.")
        ruling("2021-04-16", "If at least one target is still legal as Witherbloom Command tries to resolve, it will resolve and do as much as it can.")
        ruling("2021-04-16", "If all of Witherbloom Command's targets are illegal as it tries to resolve, it will do nothing.")
    }
}
