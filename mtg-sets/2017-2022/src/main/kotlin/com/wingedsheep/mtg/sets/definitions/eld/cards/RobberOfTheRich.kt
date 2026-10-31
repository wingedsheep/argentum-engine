package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Robber of the Rich
 * {1}{R}
 * Creature — Human Archer Rogue
 * 2/2
 * Reach, haste
 * Whenever this creature attacks, if defending player has more cards in hand than you, exile the
 * top card of their library. During any turn you attacked with a Rogue, you may cast that card and
 * you may spend mana as though it were mana of any color to cast that spell.
 *
 * The exiled card gets a permanent cast-only grant (`nonLandOnly`, "cast") gated by
 * `YouAttackedWithCreaturesThisTurn(Rogue)`. The attack record keeps each attacker as it was
 * declared, so the permission holds for the rest of that turn even after the Rogue dies or stops
 * being a Rogue, and it outlives Robber itself (both per the card's rulings).
 */
val RobberOfTheRich = card("Robber of the Rich") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Archer Rogue"
    power = 2
    toughness = 2
    oracleText = "Reach, haste\n" +
        "Whenever this creature attacks, if defending player has more cards in hand than you, " +
        "exile the top card of their library. During any turn you attacked with a Rogue, you may " +
        "cast that card and you may spend mana as though it were mana of any color to cast that spell."

    keywords(Keyword.REACH, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.DefendingPlayer, Zone.HAND),
            ComparisonOperator.GT,
            DynamicAmounts.count(Player.You, Zone.HAND)
        )
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1, player = Player.DefendingPlayer))
            exile(exiled, Player.DefendingPlayer)
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.Permanent,
                condition = Conditions.YouAttackedWithCreaturesThisTurn(
                    filter = GameObjectFilter.Creature.withSubtype("Rogue"),
                    atLeast = 1
                ),
                nonLandOnly = true,
                withAnyManaColor = true
            ))
        }
        description = "Whenever this creature attacks, if defending player has more cards in hand " +
            "than you, exile the top card of their library. During any turn you attacked with a " +
            "Rogue, you may cast that card and you may spend mana as though it were mana of any " +
            "color to cast that spell."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "138"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ecbe097-ba51-42e5-957c-382eb66c08f0.jpg?1783932619"
    }
}
