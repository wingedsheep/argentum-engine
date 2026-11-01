package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rakdos Roustabout — Ravnica Allegiance #198
 * {1}{B}{R} · Creature — Ogre Warrior · 3/2
 *
 * Whenever this creature becomes blocked, it deals 1 damage to the player or planeswalker it's
 * attacking.
 *
 * "The player or planeswalker it's attacking" is the defender itself ([EffectTarget.AttackedBy]),
 * so an attacked planeswalker takes the damage rather than its controller.
 */
val RakdosRoustabout = card("Rakdos Roustabout") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature becomes blocked, it deals 1 damage to the player or " +
        "planeswalker it's attacking."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.DealDamage(1, EffectTarget.AttackedBy(EffectTarget.Self))
        description = "Whenever this creature becomes blocked, it deals 1 damage to the player or " +
            "planeswalker it's attacking."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Lucas Graciano"
        flavorText = "\"That one has a bright future—perfect instincts for the rhythm of mayhem.\"\n—Judith"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ed534af-e16a-4d1d-8884-938fc7e13ca8.jpg?1783933641"
        ruling(
            "2019-01-25",
            "An ability that triggers when a creature becomes blocked triggers only once if two or more " +
                "creatures block it.",
        )
    }
}
