package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity

/**
 * It of the Horrid Swarm
 * {8}
 * Creature — Eldrazi Insect
 * 4/4
 *
 * Emerge {6}{G}
 * When you cast this spell, create two 1/1 green Insect creature tokens.
 *
 * Implementation notes:
 * - Emerge is the engine keyword (CR 702.119) via the `emerge(cost)` helper.
 * - The tokens come from a *cast* trigger, so they arrive while the spell is still on the stack —
 *   and they stay even if the spell is countered.
 */
val ItOfTheHorridSwarm = card("It of the Horrid Swarm") {
    manaCost = "{8}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Insect"
    power = 4
    toughness = 4
    oracleText = "Emerge {6}{G} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, create two 1/1 green Insect creature tokens."

    emerge("{6}{G}")

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
        )
        description = "When you cast this spell, create two 1/1 green Insect creature tokens."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e9160cb-d3de-49ca-a97d-2cd259cd5447.jpg?1783937526"
    }
}
