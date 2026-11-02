package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Theater of Horrors
 * {1}{B}{R}
 * Enchantment
 *
 * At the beginning of your upkeep, exile the top card of your library.
 * During your turn, if an opponent lost life this turn, you may play lands and cast spells from
 * among cards exiled with this enchantment.
 * {3}{R}: This enchantment deals 1 damage to target opponent or planeswalker.
 *
 * - The upkeep exile links the card to this enchantment (CR 607), so the play permission reads
 *   exactly "cards exiled with this enchantment" — a new Theater can't reach an old one's pile.
 * - The permission is a linked-exile grant (`GameObjectFilter.Any` admits lands, as for The
 *   Temporal Anchor) restricted to your turn and gated by the opponent-lost-life turn tracker.
 *   Normal timing still applies (ruling): sorceries and lands only in your main phase.
 */
val TheaterOfHorrors = card("Theater of Horrors") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, exile the top card of your library.\n" +
        "During your turn, if an opponent lost life this turn, you may play lands and cast spells from " +
        "among cards exiled with this enchantment.\n" +
        "{3}{R}: This enchantment deals 1 damage to target opponent or planeswalker."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val theaterTop = gather(CardSource.TopOfLibrary(1))
            exile(theaterTop, linkToSource = true)
        }
        description = "At the beginning of your upkeep, exile the top card of your library."
    }

    staticAbility {
        ability = GrantMayCastFromLinkedExile(
            filter = GameObjectFilter.Any,
            duringYourTurnOnly = true
        )
        condition = Conditions.OpponentLostLifeThisTurn
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        val victim = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
        description = "This enchantment deals 1 damage to target opponent or planeswalker."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad42efd5-79c8-44f9-b3d6-d9058e0cb0f6.jpg?1783933634"
        ruling("2019-01-25", "Damage dealt to a player causes that player to lose that much life.")
        ruling("2019-01-25", "Theater of Horrors doesn't change when you can play the exiled cards during your turn. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty. If you exile a land card, you can play it only during your main phase and only if you have an available land play remaining.")
        ruling("2019-01-25", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
        ruling("2019-01-25", "In a multiplayer game, if an opponent loses life during your turn and then leaves the game, you can play cards exiled with Theater of Horrors.")
    }
}
