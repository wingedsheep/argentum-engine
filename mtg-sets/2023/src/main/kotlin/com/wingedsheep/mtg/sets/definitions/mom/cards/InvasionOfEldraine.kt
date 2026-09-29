package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of Eldraine // Prickle Faeries — March of the Machine #113 (canonical printing).
 * {3}{B} · Battle — Siege · defense 4 // Creature — Faerie 2/2
 *
 * When this Siege enters, target opponent discards two cards.
 * // Flying. At the beginning of each opponent's upkeep, if that player has two or fewer cards in
 * //   hand, this creature deals 2 damage to them.
 *
 * The Siege reminder text restates rules every battle has (see Invasion of Innistrad), so only the
 * starting defense and the back face are declared. Prickle Faeries' "if" is an intervening-if —
 * checked when the upkeep trigger would fire and again on resolution (the Lavaborn Muse shape).
 */
private val InvasionOfEldraineFront = card("Invasion of Eldraine") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, target opponent discards two cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(2, opponent)
        description = "When this Siege enters, target opponent discards two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92f5b177-b0c6-4573-b860-f75dad1941bd.jpg?1783917011"
    }
}

/**
 * The back face. Cast transformed, for free, by the Siege's defeat trigger — so it has no mana
 * cost of its own and needs a colour indicator to be black off the battlefield.
 */
private val PrickleFaeries = card("Prickle Faeries") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Creature — Faerie"
    oracleText = "Flying\nAt the beginning of each opponent's upkeep, if that player has two or " +
        "fewer cards in hand, this creature deals 2 damage to them."
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        // "That player" is the player whose upkeep it is — bound by the step trigger.
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
            ComparisonOperator.LTE,
            2,
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "At the beginning of each opponent's upkeep, if that player has two or fewer " +
            "cards in hand, this creature deals 2 damage to them."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Cristi Balanescu"
        flavorText = "When the courts fell, Eldraine's fae turned from their usual pranks to deadlier tricks."
        imageUri = "https://cards.scryfall.io/normal/back/9/2/92f5b177-b0c6-4573-b860-f75dad1941bd.jpg?1783917011"
    }
}

val InvasionOfEldraine: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfEldraineFront,
    backFace = PrickleFaeries,
)
