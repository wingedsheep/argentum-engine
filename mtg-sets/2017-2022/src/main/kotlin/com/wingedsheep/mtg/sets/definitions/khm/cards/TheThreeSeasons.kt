package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Three Seasons
 * {G}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Mill three cards.
 * II — Return up to two target snow permanent cards from your graveyard to your hand.
 * III — Choose three cards in each graveyard. Their owners shuffle those cards into their libraries.
 *
 * Chapter III doesn't target. It walks every graveyard with the Saga's controller choosing
 * (`Chooser.SourceController` — the per-player loop rebinds "you" to the iterated player, and the
 * controller is captured from the resolving ability, so it still works if the Saga is gone) three
 * cards from it — all of them when it holds three or fewer. The chosen cards go into that player's
 * (their owner's) library, shuffled. A player whose graveyard is empty chooses nothing and doesn't
 * shuffle (ruling), hence the `ifNotEmpty` guard around the shuffle-in.
 */
val TheThreeSeasons = card("The Three Seasons") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Mill three cards.\n" +
        "II — Return up to two target snow permanent cards from your graveyard to your hand.\n" +
        "III — Choose three cards in each graveyard. Their owners shuffle those cards into their libraries."

    sagaChapter(1) {
        effect = Patterns.Library.mill(3)
    }

    sagaChapter(2) {
        targets(
            TargetFilter(GameObjectFilter.Permanent.snow().ownedByYou(), zone = Zone.GRAVEYARD),
            count = 2,
            optional = true,
        )
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
    }

    sagaChapter(3) {
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.Pipeline {
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
                val chosen = chooseExactly(
                    3,
                    from = graveyard,
                    chooser = Chooser.SourceController,
                    prompt = "Choose three cards in this graveyard to shuffle into their owner's library",
                    selectedLabel = "Shuffle in",
                )
                ifNotEmpty(chosen) {
                    move(chosen, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
                }
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48ce03b3-ec7c-46df-b89a-55405f3b5245.jpg?1783928189"
        ruling("2021-02-05", "For the chapter III ability, if a player has no cards in their graveyard, they don't shuffle their library.")
    }
}
