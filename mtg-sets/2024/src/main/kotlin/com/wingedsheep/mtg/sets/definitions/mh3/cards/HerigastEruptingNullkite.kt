package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantEmergeToOwnSpells
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Herigast, Erupting Nullkite — Modern Horizons 3 #8
 * {9} · Legendary Creature — Eldrazi Dragon · Mythic
 * 6/6
 *
 * Emerge {6}{R}{R}
 * When you cast this spell, you may exile your hand. If you do, draw three cards.
 * Flying
 * Each creature spell you cast has emerge. The emerge cost is equal to its mana cost.
 *
 * Implementation notes:
 * - The grant is `GrantEmergeToOwnSpells(Creature)`; a printed emerge (Herigast's own) wins over it.
 * - "You may exile your hand" can be chosen with an empty hand and still draws three (ruling), so it
 *   is a plain yes/no rather than an action-outcome gate.
 */
val HerigastEruptingNullkite = card("Herigast, Erupting Nullkite") {
    manaCost = "{9}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Eldrazi Dragon"
    power = 6
    toughness = 6
    oracleText = "Emerge {6}{R}{R} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, you may exile your hand. If you do, draw three cards.\n" +
        "Flying\n" +
        "Each creature spell you cast has emerge. The emerge cost is equal to its mana cost."

    emerge("{6}{R}{R}")

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.May(
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                move(hand, CardDestination.ToZone(Zone.EXILE))
            } then Effects.DrawCards(3),
            prompt = "Exile your hand and draw three cards?"
        )
        description = "When you cast this spell, you may exile your hand. If you do, draw three cards."
    }

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantEmergeToOwnSpells(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7baf9549-1869-4bd3-a52a-2f0b30ba0b16.jpg?1783911309"
        ruling("2024-06-07", "You may choose to exile your hand even if you have no cards in hand. If you do, you'll still draw three cards.")
        ruling("2024-06-07", "If a spell has more than one emerge cost, you may choose to pay it for any one of those emerge costs.")
        ruling("2024-06-07", "Once you begin to cast a spell using an emerge ability granted by Herigast, losing control of Herigast won't affect it. You can finish casting it as normal. For example, you can choose to sacrifice Herigast as you choose to pay a creature spell's emerge cost from an emerge ability granted by Herigast itself.")
        ruling("2024-06-07", "Herigast's triggered ability will resolve before Herigast does. If Herigast is countered or otherwise leaves the stack in response to its triggered ability, the triggered ability will still resolve as normal.")
    }
}
