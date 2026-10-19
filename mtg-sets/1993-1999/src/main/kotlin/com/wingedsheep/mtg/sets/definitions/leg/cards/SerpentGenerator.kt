package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serpent Generator
 * {6}
 * Artifact
 * {4}, {T}: Create a 1/1 colorless Snake artifact creature token. It has "Whenever this creature
 * deals damage to a player, that player gets a poison counter."
 *
 * The token's quoted ability is its own triggered ability — Marsh Viper's any-damage trigger (not
 * combat-only, not infect/toxic) with a count of one. Legends has no token set on Scryfall and no
 * printing of this Snake token exists, so the token falls through to the engine's generic art.
 */
val SerpentGenerator = card("Serpent Generator") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "{4}, {T}: Create a 1/1 colorless Snake artifact creature token. It has \"Whenever " +
        "this creature deals damage to a player, that player gets a poison counter.\" (A player with " +
        "ten or more poison counters loses the game.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Snake"),
            artifactToken = true,
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.dealsDamage(Recipient.AnyPlayer),
                    effect = Effects.AddCounters(
                        counterType = CounterType.POISON,
                        count = 1,
                        target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                    ),
                ),
            ),
        )
        description = "{4}, {T}: Create a 1/1 colorless Snake artifact creature token with " +
            "\"Whenever this creature deals damage to a player, that player gets a poison counter.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "295"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c350c38-6cbb-4b8b-823f-45d6a16568cc.jpg?1783948026"
    }
}
