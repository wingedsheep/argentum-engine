package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Cruelty of Gix
 * {3}{B}{B}
 * Enchantment — Saga
 *
 * Read ahead
 * I — Target opponent reveals their hand. You choose a creature or planeswalker card from it.
 *     That player discards that card. (Despise's shape.)
 * II — Search your library for a card, put that card into your hand, then shuffle. You lose 3
 *      life. (Grim Tutor's shape; the life loss is unconditional.)
 * III — Put target creature card from a graveyard onto the battlefield under your control.
 */
val TheCrueltyOfGix = card("The Cruelty of Gix") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — Target opponent reveals their hand. You choose a creature or planeswalker card from " +
        "it. That player discards that card.\n" +
        "II — Search your library for a card, put that card into your hand, then shuffle. You " +
        "lose 3 life.\n" +
        "III — Put target creature card from a graveyard onto the battlefield under your control."

    readAhead()

    sagaChapter(1) {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Creature or GameObjectFilter.Planeswalker,
                prompt = "Choose a creature or planeswalker card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
    }

    sagaChapter(2) {
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY), search = true)
            val found = chooseExactly(1, from = library, prompt = "Choose a card to put into your hand")
            toHand(found)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        } then Effects.LoseLife(3, EffectTarget.Controller)
    }

    sagaChapter(3) {
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(
            creature,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d6e5eb3-25ab-4e9e-96e8-655e0ce74675.jpg?1783921334"

        ruling("2022-09-09", "You must search your library as the chapter II ability resolves, and you will lose 3 life.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
