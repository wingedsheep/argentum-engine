package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity

/**
 * Make Mischief — Eldritch Moon #135
 * {2}{R} · Sorcery
 *
 * Make Mischief deals 1 damage to any target. Create a 1/1 red Devil creature token. It has
 * "When this token dies, it deals 1 damage to any target."
 *
 * The Devil token is the same one Dance with Devils makes: its death trigger is a token-level
 * triggered ability, targeting on the stack and dealing damage from the dead token via
 * last-known information.
 */
val MakeMischief = card("Make Mischief") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Make Mischief deals 1 damage to any target. Create a 1/1 red Devil creature token. " +
        "It has \"When this token dies, it deals 1 damage to any target.\""

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Devil"),
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
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Ben Maier"
        flavorText = "A tidy desk is as good as an invitation."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2049072-5901-4edd-8305-ce55f256bca5.jpg?1783937459"
    }
}
