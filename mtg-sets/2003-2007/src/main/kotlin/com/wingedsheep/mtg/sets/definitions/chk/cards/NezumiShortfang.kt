package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nezumi Shortfang // Stabwhisker the Odious (Champions of Kamigawa #131) — a flip card (CR 710).
 *
 * Nezumi Shortfang {1}{B} — Creature — Rat Rogue 1/1
 * "{1}{B}, {T}: Target opponent discards a card. Then if that player has no cards in hand, flip
 * this creature."
 *
 * Stabwhisker the Odious — Legendary Creature — Rat Shaman 3/3
 * "At the beginning of each opponent's upkeep, that player loses 1 life for each card fewer than
 * three in their hand."
 *
 * The flip check reads the targeted opponent's hand after the discard (an already-empty hand still
 * flips it). Stabwhisker's loss is max(0, 3 − hand size), counted as the trigger resolves.
 */
private val NezumiShortfangUpright = card("Nezumi Shortfang") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Rogue"
    oracleText = "{1}{B}, {T}: Target opponent discards a card. Then if that player has no cards in hand, " +
        "flip this creature."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(1, opponent) then
            Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.count(opponent.asPlayer, Zone.HAND),
                    ComparisonOperator.EQ,
                    0,
                ),
                Effects.Flip(),
            )
        description = "{1}{B}, {T}: Target opponent discards a card. Then if that player has no cards in " +
            "hand, flip this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8265c39-d287-4c5a-baba-f2f09dd80a1c.jpg?1783944311"
    }
}

private val StabwhiskerTheOdious = card("Stabwhisker the Odious") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Rat Shaman"
    oracleText = "At the beginning of each opponent's upkeep, that player loses 1 life for each card fewer " +
        "than three in their hand."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        // "That player" is the player whose upkeep it is — bound by the step trigger.
        effect = Effects.LoseLife(
            DynamicAmounts.max(
                DynamicAmounts.fixed(0),
                3 - DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
            ),
            EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "At the beginning of each opponent's upkeep, that player loses 1 life for each card " +
            "fewer than three in their hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8265c39-d287-4c5a-baba-f2f09dd80a1c.jpg?1783944311"
    }
}

val NezumiShortfang: CardDefinition = CardDefinition.flipCard(
    unflipped = NezumiShortfangUpright,
    flipped = StabwhiskerTheOdious,
)
