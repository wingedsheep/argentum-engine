package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nicol Bolas, the Deceiver — Hour of Devastation #205 (Planeswalker Deck)
 * {5}{U}{B}{R} · Legendary Planeswalker — Bolas · Loyalty 5
 *
 * +3: Each opponent loses 3 life unless that player sacrifices a nonland permanent of their
 *     choice or discards a card.
 * −3: Destroy target creature. Draw a card.
 * −11: Nicol Bolas deals 7 damage to each opponent. You draw seven cards.
 *
 * +3 is Torment of Hailfire's process, run once: inside [Effects.ForEachPlayer] the iterating
 * opponent is the [EffectTarget.Controller], who chooses, pays from their own permanents or hand,
 * or loses the life. An option they can't afford isn't offered, and losing 3 life is always
 * available (ruling). −3 is Garruk, Cursed Huntsman's −3: the draw is unconditional once the
 * ability resolves, so an indestructible target still draws a card.
 */
val NicolBolasTheDeceiver = card("Nicol Bolas, the Deceiver") {
    manaCost = "{5}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Planeswalker — Bolas"
    startingLoyalty = 5
    oracleText = "+3: Each opponent loses 3 life unless that player sacrifices a nonland permanent of their " +
        "choice or discards a card.\n" +
        "−3: Destroy target creature. Draw a card.\n" +
        "−11: Nicol Bolas deals 7 damage to each opponent. You draw seven cards."

    loyaltyAbility(+3) {
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.PayOrSuffer(
                cost = Costs.pay.Choice(
                    listOf(
                        Costs.pay.Sacrifice(filter = GameObjectFilter.NonlandPermanent),
                        Costs.pay.Discard()
                    )
                ),
                suffer = Effects.LoseLife(3, target = EffectTarget.Controller),
                player = EffectTarget.Controller,
                // Asked of each opponent, so the consequence is in their words.
                consequenceDescription = "lose 3 life"
            )
        )
        description = "Each opponent loses 3 life unless that player sacrifices a nonland permanent of their " +
            "choice or discards a card."
    }

    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.DrawCards(1)
        description = "Destroy target creature. Draw a card."
    }

    loyaltyAbility(-11) {
        effect = Effects.DealDamage(7, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.DrawCards(7)
        description = "Nicol Bolas deals 7 damage to each opponent. You draw seven cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "205"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74239d78-1608-4b27-b2fd-8c7453f6b86b.jpg?1783935986"
        inBooster = false
        ruling(
            "2017-07-14",
            "While resolving Nicol Bolas's first ability, your opponent chooses a card to be discarded without " +
                "revealing it, chooses a nonland permanent to be sacrificed, or chooses to do neither. Then that " +
                "player discards that card, sacrifices that permanent, or loses 3 life. Your opponent can always " +
                "choose to lose 3 life, even if they have cards to discard or nonland permanents to sacrifice."
        )
        ruling(
            "2017-07-14",
            "If the target of Nicol Bolas's second ability becomes illegal, the ability doesn't resolve and you " +
                "won't draw a card. If that target is legal but can't be destroyed, most likely because it has " +
                "indestructible, you still draw a card."
        )
        ruling(
            "2017-07-14",
            "If Nicol Bolas's third ability causes each opponent to have 0 or less life, but it also causes you to " +
                "try to draw more cards than you have in your library, the game ends in a draw."
        )
    }
}
