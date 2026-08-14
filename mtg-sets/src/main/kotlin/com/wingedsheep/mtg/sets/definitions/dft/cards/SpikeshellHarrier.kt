package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Spikeshell Harrier — Aetherdrift #65
 * {4}{U} · Artifact Creature — Robot Turtle · 4/4
 *
 * When this creature enters, return target creature or Vehicle an opponent controls to its owner's
 * hand. If that opponent's speed is greater than each other player's speed, reduce that opponent's
 * speed by 1. This effect can't reduce their speed below 1.
 *
 * "That opponent" is the bounced permanent's *controller*, so both halves hang off the one target:
 * [Player.ControllerOf] resolves from the captured target id, and its last-known controller survives
 * the bounce (the resolver falls through the permanent's last-known snapshot before owner), which is
 * what makes it still name the right player once the card is in hand — the same idiom Unwanted
 * Remake uses for "its controller" after a destroy.
 *
 * **"Greater than each other player's speed"** is a max-over-all-players comparison, which a binary
 * `Compare` can't express (a `Speed(EachOpponent)` reads only the first opponent). It is instead
 * counted: *exactly one* player has speed ≥ that opponent's speed iff that opponent is the strict,
 * unique leader — they always satisfy the inner comparison themselves, so a count of 1 means nobody
 * else ties or beats them. [DynamicAmount.CountPlayersWith] rebinds the controller per candidate, so
 * `Speed(Player.You)` inside the loop is each player's own speed while `Speed(thatOpponent)` keeps
 * reading the bounced permanent's controller.
 *
 * The floor is the effect's own, not a rule: [Effects.ReduceSpeed] takes [Speed.STARTING] as its
 * `minimum`, and `SpeedService` never *grants* speed to a player who has none, so an opponent with no
 * speed is left alone rather than being pushed up to 1.
 */
val SpikeshellHarrier = card("Spikeshell Harrier") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Turtle"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, return target creature or Vehicle an opponent controls " +
        "to its owner's hand. If that opponent's speed is greater than each other player's speed, " +
        "reduce that opponent's speed by 1. This effect can't reduce their speed below 1."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val bounced = target(
            "target creature or Vehicle an opponent controls",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle.opponentControls()))
        )
        val thatOpponent = Player.ControllerOf("target creature or Vehicle an opponent controls")

        effect = Effects.Composite(
            Effects.ReturnToHand(bounced),
            ConditionalEffect(
                condition = Compare(
                    left = DynamicAmount.CountPlayersWith(
                        scope = Player.Each,
                        condition = Compare(
                            left = DynamicAmount.Speed(Player.You),
                            operator = ComparisonOperator.GTE,
                            right = DynamicAmount.Speed(thatOpponent)
                        )
                    ),
                    operator = ComparisonOperator.EQ,
                    right = DynamicAmount.Fixed(1)
                ),
                effect = Effects.ReduceSpeed(
                    amount = DynamicAmount.Fixed(1),
                    target = EffectTarget.PlayerRef(thatOpponent),
                    minimum = Speed.STARTING
                )
            )
        )
        description = "When this creature enters, return target creature or Vehicle an opponent " +
            "controls to its owner's hand. If that opponent's speed is greater than each other " +
            "player's speed, reduce that opponent's speed by 1. This effect can't reduce their " +
            "speed below 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Alfonso Santano"
        flavorText = "\"Target acquired. Resolution: Imminent impact.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f1ece22-ca32-45bb-b5f4-480f9b366cb5.jpg?1783907901"
    }
}
