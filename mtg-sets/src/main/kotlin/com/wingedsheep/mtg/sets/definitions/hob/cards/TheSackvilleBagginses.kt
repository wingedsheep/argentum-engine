package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * The Sackville-Bagginses — The Hobbit #83
 * {1}{B} · Legendary Creature — Halfling Citizen · Rare
 * 2/2
 *
 * When The Sackville-Bagginses enter, you may sacrifice another creature or artifact.
 * If you do, draw a card and create a Treasure token.
 * Whenever you sacrifice a token, target opponent loses 1 life.
 *
 * The "you may sacrifice another creature or artifact. If you do, …" clause is the Swarm Culler /
 * Comet Crawler shell — a declared permanent under a [MayEffect], so the "if you do" rider is bound to
 * a sacrifice that is guaranteed to happen once the controller accepts. With no other creature or
 * artifact on the battlefield there is nothing to declare and the trigger simply does nothing.
 *
 * The second ability is a per-permanent sacrifice trigger ([Triggers.YouSacrificeA]), so sacrificing
 * three tokens at once fires it three times (CR 603.2c) rather than once — and it fires for *any*
 * token, including the Treasure this card just made and the Sackville-Bagginses themselves if they
 * happen to be a token copy.
 */
val TheSackvilleBagginses = card("The Sackville-Bagginses") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Halfling Citizen"
    power = 2
    toughness = 2
    oracleText = "When The Sackville-Bagginses enter, you may sacrifice another creature or " +
        "artifact. If you do, draw a card and create a Treasure token.\n" +
        "Whenever you sacrifice a token, target opponent loses 1 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val sacrificed = target(
            "another creature or artifact",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Creature.youControl().or(GameObjectFilter.Artifact.youControl())
                ).other()
            )
        )
        effect = MayEffect(
            Effects.SacrificeTarget(sacrificed) then
                Effects.DrawCards(1) then
                Effects.CreateTreasure()
        )
        description = "When The Sackville-Bagginses enter, you may sacrifice another creature or " +
            "artifact. If you do, draw a card and create a Treasure token."
    }

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Any.token())
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.LoseLife(1, opponent)
        description = "Whenever you sacrifice a token, target opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Denman Rooke"
        flavorText = "It took more than Bilbo showing up alive to convince some that he was not, in " +
            "fact, \"presumed dead.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed87b471-79f9-45ec-9188-69e970f6121e.jpg?1784894871"
    }
}
