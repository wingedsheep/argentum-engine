package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Goblin Goliath (PZ2 #70783)
 * {4}{R}{R}  Creature — Goblin Mutant  5/4
 *
 * When this creature enters, create a number of 1/1 red Goblin creature tokens equal to the number
 * of opponents you have.
 * {3}{R}, {T}: If a source you control would deal damage to an opponent this turn, it deals double
 * that damage to that player instead.
 *
 * First printed in the Game Night (GNT) box set, which isn't scaffolded as a set; its first
 * scaffoldable printing is the Treasure Chest (PZ2), mirroring Immortal Phoenix and Angelic Guardian.
 *
 * The token count reads `PlayerCount(EachOpponent)` at resolution. The activated ability installs an
 * until-end-of-turn doubling replacement (CR 614) controlled by the activator: it matches any source
 * that player controls when the damage is dealt (tokens made later included) and only damage dealt
 * to an opponent — damage to creatures or planeswalkers an opponent controls is not doubled. It is a
 * floating effect, so it keeps working if Goblin Goliath leaves the battlefield.
 */
val GoblinGoliath = card("Goblin Goliath") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Mutant"
    oracleText = "When this creature enters, create a number of 1/1 red Goblin creature tokens equal to " +
        "the number of opponents you have.\n" +
        "{3}{R}, {T}: If a source you control would deal damage to an opponent this turn, it deals " +
        "double that damage to that player instead."
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.playerCount(Player.EachOpponent),
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/7/0/70f8a1de-cd4c-4afa-bf03-0245d375d42e.jpg?1783908589",
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}"), Costs.Tap)
        effect = Effects.DoubleDamageThisTurn(
            EventPattern.DamageEvent(
                recipient = Recipient.Opponent,
                source = GameObjectFilter.Any.youControl(),
            )
        )
        description = "Double the damage sources you control deal to opponents this turn"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70783"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcaf7d1e-9ebf-497e-9d0e-bd23d3bf2b12.jpg?1783933914"
    }
}
