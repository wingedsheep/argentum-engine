package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasion of Kaldheim // Pyre of the World Tree — March of the Machine #145.
 * {3}{R} · Battle — Siege · defense 4 // Enchantment
 *
 * Front: gather the hand, exile it, grant "may play until the end of your next turn" over exactly
 * those cards, then draw as many as were exiled (the count is read from the pipeline collection).
 * Back: a discard-a-land cost for 2 damage, plus a "whenever you discard a land card" impulse of
 * the top card of the library for this turn.
 */
private val InvasionOfKaldheimFront = card("Invasion of Kaldheim") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, exile all cards from your hand, then draw that many cards. Until " +
        "the end of your next turn, you may play cards exiled this way."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
            exile(hand)
            run(Effects.GrantMayPlayFromExile(hand, MayPlayExpiry.UntilEndOfNextTurn))
            run(Effects.DrawCards(hand.count))
        }
        description = "When this Siege enters, exile all cards from your hand, then draw that many " +
            "cards. Until the end of your next turn, you may play cards exiled this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7cedd62-efc8-464d-8387-220bb55e07e9.jpg?1783916996"
        ruling("2023-04-14", "Both Invasion of Kaldheim and Pyre of the World Tree have an ability that allow you to play cards from exile. You may play those cards during the specified duration even if the permanent with the ability leaves the battlefield or you lose control of it.")
        ruling("2023-04-14", "Any cards you don't play will remain exiled.")
    }
}

private val PyreOfTheWorldTree = card("Pyre of the World Tree") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Enchantment"
    oracleText = "Discard a land card: This enchantment deals 2 damage to any target.\n" +
        "Whenever you discard a land card, exile the top card of your library. You may play that card this turn."

    activatedAbility {
        cost = Costs.Discard(GameObjectFilter.Land)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Land)
        effect = Patterns.Exile.impulse(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Bryan Sola"
        flavorText = "The warriors of Kaldheim burned the soul of their world to keep it out of Phyrexia's hands."
        imageUri = "https://cards.scryfall.io/normal/back/f/7/f7cedd62-efc8-464d-8387-220bb55e07e9.jpg?1783916996"
        ruling("2023-04-14", "The last ability of Pyre of the World Tree will trigger whenever you discard a land card for any reason, not just because you activated its other ability.")
    }
}

val InvasionOfKaldheim: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfKaldheimFront,
    backFace = PyreOfTheWorldTree,
)
