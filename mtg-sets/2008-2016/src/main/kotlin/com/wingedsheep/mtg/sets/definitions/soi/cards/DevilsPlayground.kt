package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity

/**
 * Devils' Playground — Shadows over Innistrad #151
 * {4}{R}{R} · Sorcery
 *
 * Create four 1/1 red Devil creature tokens. They have "When this token dies, it deals 1 damage to
 * any target."
 *
 * Same token as [DanceWithDevils]: each Devil carries its own death trigger, the target is chosen
 * when the trigger goes on the stack, and the dead token is the damage source.
 */
val DevilsPlayground = card("Devils' Playground") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create four 1/1 red Devil creature tokens. They have \"When this token dies, it " +
        "deals 1 damage to any target.\""

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Devil"),
            count = 4,
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    val any = target(Targets.Any)
                    effect = Effects.DealDamage(1, any)
                    description = "When this token dies, it deals 1 damage to any target."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/3/e/3e78c4b8-371b-43d7-a315-fb299704aa60.jpg?1783937682",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Wayne England"
        flavorText = "\"The city is overrun. I would say burn it to the ground, but that won't be " +
            "necessary.\"\n—Inquisitor Delmund"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/1207e6eb-15a7-4ec4-91b3-880375704bb6.jpg?1783937756"
    }
}
