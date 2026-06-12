package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Weftwalking — Edge of Eternities #86
 * {4}{U}{U} · Enchantment · Mythic
 *
 * When this enchantment enters, if you cast it, shuffle your hand and graveyard into your library,
 * then draw seven cards.
 * The first spell each player casts during each of their turns may be cast without paying its mana cost.
 *
 * - The ETB is an intervening-if trigger ([Conditions.WasCast]) — only fires when Weftwalking is cast,
 *   not when put onto the battlefield by reanimation or similar non-cast means.
 * - The static grants every player a "may cast without paying its mana cost" alternative on the first
 *   spell they cast during each of their own turns. Engine wiring lives in
 *   [CostCalculator.hasFreeCastPermission] (gate check) and [CastSpellHandler] (free-cast resolution
 *   via [CastSpell.useWithoutPayingManaCost]).
 */
val Weftwalking = card("Weftwalking") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, if you cast it, shuffle your hand and graveyard into your library, then draw seven cards.\n" +
        "The first spell each player casts during each of their turns may be cast without paying its mana cost."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        effect = Effects.Pipeline {
            // Gather hand + graveyard in a single pass (one combined "shuffle ... into your library"
            // event per oracle text) and shuffle the result into the controller's library.
            val weftwalkingShuffleCards = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                    player = Player.You
                ),
                name = "weftwalkingShuffleCards"
            )
            move(
                weftwalkingShuffleCards,
                destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled)
            )
            run(Effects.DrawCards(7))
        }
    }

    staticAbility {
        ability = MayCastWithoutPayingManaCost(firstSpellOfTurnOnly = true)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "86"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39d48ddd-4529-4284-9da3-5272ad362b9b.jpg?1752946899"
        ruling(
            "2025-07-25",
            "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. " +
                "You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, " +
                "such as that of Embrace Oblivion, those must be paid to cast the spell."
        )
    }
}
