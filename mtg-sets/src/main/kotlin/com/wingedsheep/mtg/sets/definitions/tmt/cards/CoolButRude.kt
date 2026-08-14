package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cool but Rude
 * {1}{R}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * Whenever you attack, you may discard a card. If you do, draw a card.
 * {1}{R}: Level 2
 * Whenever you discard a card, this Class deals 2 damage to each opponent.
 * {1}{R}: Level 3
 * When this Class becomes level 3, search your library for a card, put it into
 * your hand, shuffle, then discard a card at random.
 */
val CoolButRude = card("Cool but Rude") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "Whenever you attack, you may discard a card. If you do, draw a card.\n" +
        "{1}{R}: Level 2\nWhenever you discard a card, this Class deals 2 damage to each opponent.\n" +
        "{1}{R}: Level 3\nWhen this Class becomes level 3, search your library for a card, put it into your hand, shuffle, then discard a card at random."

    // Level 1: Whenever you attack, you may discard a card. If you do, draw a card.
    // The draw hangs off `IfYouDo`, not off the may-decision: discarding is moving a card from hand
    // to graveyard (CR 701.9a), so an empty hand discards nothing and nothing is drawn.
    // `feasibility` keeps the trigger from asking an unanswerable question on every single attack.
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = MayEffect(
            effect = Effects.IfYouDo(
                action = Patterns.Hand.discardCards(1),
                ifYouDo = Effects.DrawCards(1)
            ),
            feasibility = FeasibilityCheck.HasCardsInZone(Zone.HAND)
        )
    }

    // Level 2: Whenever you discard a card, this Class deals 2 damage to each opponent.
    classLevel(2, "{1}{R}") {
        triggeredAbility {
            trigger = Triggers.YouDiscard
            effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }

    // Level 3: becomes level 3 (EntersBattlefield inside the level block) — tutor any card
    // to hand, shuffle, then discard a card at random.
    classLevel(3, "{1}{R}") {
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true
            ).then(Patterns.Hand.discardRandom(1))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Lordigan"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a566ab2d-6ec8-4833-8ad6-210378b1a20e.jpg?1777939762"
    }
}
