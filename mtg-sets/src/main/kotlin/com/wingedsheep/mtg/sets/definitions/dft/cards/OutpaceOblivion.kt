package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker

/**
 * Outpace Oblivion — Aetherdrift #139
 * {2}{R} · Enchantment
 *
 * Start your engines!
 * When this enchantment enters, it deals 5 damage to up to one target creature or planeswalker.
 * {2}, Sacrifice this enchantment: It deals 2 damage to each player who doesn't have max speed.
 *
 * "Each player who doesn't have max speed" is the per-player evaluation Momentum Breaker uses:
 * one [ForEachPlayerEffect] over [Player.Each] whose body is a [ConditionalEffect]. Inside the
 * loop the controller is rebound to the iterated player, so `Not(HasMaxSpeed(Player.You))` asks
 * about *that* player and [EffectTarget.Controller] is that player — which keeps the check right
 * in multiplayer and includes Outpace Oblivion's own controller, exactly as printed.
 *
 * Whether a player has max speed is checked as the ability resolves, one iteration at a time; the
 * damage can't change anyone's speed (speed only rises, on your own turn, when an opponent loses
 * life — CR 702.179d), so the sequential iteration matches the simultaneous printed damage.
 *
 * The enchantment is already in the graveyard when the ability resolves (it was sacrificed to pay
 * the cost), so the damage comes from its last-known information — the engine's default source
 * handling for a sacrificed cost, no explicit `damageSource` needed.
 */
val OutpaceOblivion = card("Outpace Oblivion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "When this enchantment enters, it deals 5 damage to up to one target creature or planeswalker.\n" +
        "{2}, Sacrifice this enchantment: It deals 2 damage to each player who doesn't have max speed."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "up to one target creature or planeswalker",
            TargetCreatureOrPlaneswalker(optional = true),
        )
        effect = Effects.DealDamage(5, t)
        description = "When this enchantment enters, it deals 5 damage to up to one target " +
            "creature or planeswalker."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                ConditionalEffect(
                    condition = Conditions.Not(Conditions.HasMaxSpeed(Player.You)),
                    effect = Effects.DealDamage(2, EffectTarget.Controller),
                ),
            ),
        )
        description = "{2}, Sacrifice this enchantment: It deals 2 damage to each player who " +
            "doesn't have max speed."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c22e415f-636f-4394-9b21-600ab720ac98.jpg?1783907878"
        ruling(
            "2025-02-07",
            "If an effect needs to know what a player's speed is and that player doesn't have a " +
                "speed, their speed is considered 0.",
        )
        ruling("2025-02-07", "A player \"has max speed\" if their speed is 4.")
    }
}
