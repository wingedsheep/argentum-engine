package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ghostly Pilferer
 * {1}{U}
 * Creature — Spirit Rogue
 * 2/1
 * Whenever this creature becomes untapped, you may pay {2}. If you do, draw a card.
 * Whenever an opponent casts a spell from anywhere other than their hand, draw a card.
 * Discard a card: This creature can't be blocked this turn.
 */
val GhostlyPilferer = card("Ghostly Pilferer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit Rogue"
    oracleText = "Whenever this creature becomes untapped, you may pay {2}. If you do, draw a card.\n" +
        "Whenever an opponent casts a spell from anywhere other than their hand, draw a card.\n" +
        "Discard a card: This creature can't be blocked this turn."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.DrawCards(1),
        )
        description = "Whenever this creature becomes untapped, you may pay {2}. If you do, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(requires = setOf(SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND)))
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent casts a spell from anywhere other than their hand, draw a card."
    }

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Discard a card: This creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2810631f-c55c-4947-a26f-4d3ce76024b3.jpg?1783930726"
        ruling("2020-06-23", "Ghostly Pilferer's first ability triggers during your untap step, but it's put onto the stack at the same time as abilities that trigger at the beginning of your upkeep step. Even though the ability triggered first, you may order it before or after other abilities you control that are put onto the stack at this time.")
        ruling("2020-06-23", "While resolving Ghostly Pilferer's first ability, you can't pay more than {2} to draw more than one card.")
        ruling("2020-06-23", "Once Ghostly Pilferer has been blocked, activating its last ability doesn't cause it to become unblocked.")
    }
}
