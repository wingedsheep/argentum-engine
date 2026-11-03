package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stray from History — Star Trek #130
 * {2}{B} · Sorcery
 *
 * Choose one —
 * • Rewrite the Past — Return one or two target creature and/or Spacecraft cards from your graveyard
 *   to your hand.
 * • Repair the Timeline — Target opponent reveals their hand. You choose a nonland card from it.
 *   Exile that card.
 *
 * Rewrite the Past is a one-to-two target requirement (Counterintelligence's shape) over creature or
 * Spacecraft cards you own in your graveyard. Repair the Timeline is Check for Traps' reveal → choose
 * a nonland card → exile pipeline without the life-loss rider.
 */
val StrayFromHistory = card("Stray from History") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Rewrite the Past — Return one or two target creature and/or Spacecraft cards from your " +
        "graveyard to your hand.\n" +
        "• Repair the Timeline — Target opponent reveals their hand. You choose a nonland card from it. " +
        "Exile that card."

    spell {
        modal(chooseCount = 1) {
            mode("Rewrite the Past — Return one or two target creature and/or Spacecraft cards from your graveyard to your hand") {
                targets(
                    TargetFilter(
                        (GameObjectFilter.Creature or GameObjectFilter.Any.withSubtype("Spacecraft")).ownedByYou(),
                        zone = Zone.GRAVEYARD
                    ),
                    count = 2,
                    minCount = 1
                )
                effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
            }

            mode("Repair the Timeline — Target opponent reveals their hand. You choose a nonland card from it. Exile that card") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    run(Effects.RevealHand(opponent))
                    val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                    val chosen = chooseExactly(
                        1,
                        from = opponentHand,
                        chooser = Chooser.Controller,
                        filter = GameObjectFilter.Nonland,
                        prompt = "Choose a nonland card to exile",
                        alwaysPrompt = true,
                        showAllCards = true
                    )
                    exile(chosen, opponent.asPlayer)
                }
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Joel Chaim Holtzman"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aac57a7f-a28b-486a-8d85-c620702a9cc0.jpg?1793359454"
    }
}
