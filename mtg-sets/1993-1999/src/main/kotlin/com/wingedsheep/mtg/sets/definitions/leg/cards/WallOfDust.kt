package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wall of Dust
 * {2}{R}
 * Creature — Wall
 * 1/4
 *
 * Defender (This creature can't attack.)
 * Whenever this creature blocks a creature, that creature can't attack during its controller's
 * next turn.
 */
val WallOfDust = card("Wall of Dust") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wall"
    power = 1
    toughness = 4
    oracleText = "Defender (This creature can't attack.)\n" +
        "Whenever this creature blocks a creature, that creature can't attack during its controller's next turn."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        // One trigger per blocked creature, with that attacker as the triggering entity.
        trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature)
        effect = Effects.CantAttack(EffectTarget.TriggeringEntity, Duration.EndOfAffectedControllersNextTurn)
        description = "Whenever this creature blocks a creature, that creature can't attack during its " +
            "controller's next turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Richard Thomas"
        flavorText = "An ever-moving swarm of dust engulfs and disorients anything that comes near."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaf53dfe-5d48-4811-b2f5-5a5c1cb462ca.jpg?1783948051"
    }
}
