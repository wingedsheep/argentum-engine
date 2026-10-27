package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Within Range
 * {3}{B}
 * Enchantment
 *
 * When this enchantment enters, create two 1/1 red Warrior creature tokens.
 * Whenever you attack, each opponent loses life equal to the number of creatures attacking them.
 *
 * `ForEachPlayer(EachOpponent)` rebinds `Controller` to the visited opponent (Netherborn Phalanx,
 * Ainok Strike Leader), so the count of creatures `attackingPlayer(Controller)` — Sentry Bot's
 * "creatures attacking you" filter — is the number attacking *that* opponent, and the life loss
 * lands on them. Attackers aimed at a planeswalker or battle don't count; an opponent nobody
 * attacked loses 0.
 */
val WithinRange = card("Within Range") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create two 1/1 red Warrior creature tokens.\n" +
        "Whenever you attack, each opponent loses life equal to the number of creatures attacking them."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Warrior"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/7/e/7edc0515-a130-45a7-aa09-0e23bba41587.jpg?1783906783"
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.LoseLife(
                DynamicAmounts.battlefield(
                    Player.Each,
                    GameObjectFilter.Creature.attackingPlayer(EffectTarget.Controller),
                ).count(),
                EffectTarget.Controller
            )
        )
        description = "Whenever you attack, each opponent loses life equal to the number of creatures attacking them."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Hector Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51188edb-9705-4960-854b-170744f40a88.jpg?1783907174"
    }
}
