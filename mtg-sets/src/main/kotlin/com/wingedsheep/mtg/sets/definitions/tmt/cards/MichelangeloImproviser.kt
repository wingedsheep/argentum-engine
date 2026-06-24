package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Michelangelo, Improviser
 * {3}{G}
 * Legendary Creature — Mutant Ninja Turtle
 * 4/4
 *
 * Sneak {2}{G}{G} (You may cast this spell for {2}{G}{G} if you also return an unblocked
 * attacker you control to hand during the declare blockers step. He enters tapped and
 * attacking.)
 * Whenever Michelangelo deals combat damage to a player, you may put a creature card and/or a
 * land card from your hand onto the battlefield.
 *
 * "A creature card and/or a land card" is two independent up-to-one selections — one over
 * creature cards in hand, one over land cards — composed in sequence (`Patterns.Hand.putFromHand`
 * with `count = 1`, which uses a `ChooseUpTo(1)` selection). Because each selection allows
 * picking zero, every combination the "and/or … you may" wording permits is reachable: a
 * creature only, a land only, both, or neither (declining outright).
 */
val MichelangeloImproviser = card("Michelangelo, Improviser") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Sneak {2}{G}{G} (You may cast this spell for {2}{G}{G} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nWhenever Michelangelo deals combat damage to a player, you may put a creature card and/or a land card from your hand onto the battlefield."
    power = 4
    toughness = 4

    sneak("{2}{G}{G}")

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            Patterns.Hand.putFromHand(GameObjectFilter.Creature, count = 1),
            Patterns.Hand.putFromHand(GameObjectFilter.Land, count = 1)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "119"
        artist = "Narendra Bintara Adi"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/955848c0-5092-4e13-97c9-5978d44d5586.jpg?1769006201"
    }
}
