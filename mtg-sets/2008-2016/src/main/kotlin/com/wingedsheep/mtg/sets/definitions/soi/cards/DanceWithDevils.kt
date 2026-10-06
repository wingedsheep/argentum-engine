package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Dance with Devils — Shadows over Innistrad #150
 * {3}{R} · Instant
 *
 * Create two 1/1 red Devil creature tokens. They have "When this token dies, it deals 1 damage to
 * any target."
 *
 * The Devils carry their death trigger as a token-level [TriggeredAbility], so each token is
 * self-contained (a copy keeps the trigger). The target is chosen when the trigger is put on the
 * stack; the damage source is the dead token, read via last-known information.
 */
val DanceWithDevils = card("Dance with Devils") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Create two 1/1 red Devil creature tokens. They have \"When this token dies, it " +
        "deals 1 damage to any target.\""

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Devil"),
            count = 2,
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Wayne England"
        flavorText = "Devils sow chaos and reap panic."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1112ebe-2a42-48b9-b0e5-f708305d088a.jpg?1783937757"
    }
}
