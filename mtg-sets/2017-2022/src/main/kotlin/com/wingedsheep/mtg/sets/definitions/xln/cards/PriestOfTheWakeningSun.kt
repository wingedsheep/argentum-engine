package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Priest of the Wakening Sun
 * {W}
 * Creature — Human Cleric
 * 1/1
 * At the beginning of your upkeep, you may reveal a Dinosaur card from your hand. If you do, you
 * gain 2 life.
 * {3}{W}{W}, Sacrifice this creature: Search your library for a Dinosaur card, reveal it, put it
 * into your hand, then shuffle.
 *
 * The optional reveal is a hand gather → `chooseUpTo(1)` → reveal pipeline; the life gain runs only
 * when a card was actually chosen (`ifNotEmpty`), so declining or holding no Dinosaur gains nothing.
 * The revealed card stays in hand.
 */
val PriestOfTheWakeningSun = card("Priest of the Wakening Sun") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your upkeep, you may reveal a Dinosaur card from your hand. " +
        "If you do, you gain 2 life.\n" +
        "{3}{W}{W}, Sacrifice this creature: Search your library for a Dinosaur card, reveal it, " +
        "put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val dinosaurs = gather(
                CardSource.FromZone(
                    zone = Zone.HAND,
                    player = Player.You,
                    filter = GameObjectFilter.Any.withSubtype("Dinosaur")
                )
            )
            val revealed = chooseUpTo(
                1,
                from = dinosaurs,
                prompt = "You may reveal a Dinosaur card from your hand"
            )
            reveal(revealed, revealToSelf = false)
            ifNotEmpty(revealed) {
                run(Effects.GainLife(2))
            }
        }
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}{W}"), Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype("Dinosaur"),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/051dc73e-f04a-4901-b0ed-3b33c33ac72f.jpg?1783935795"
    }
}
