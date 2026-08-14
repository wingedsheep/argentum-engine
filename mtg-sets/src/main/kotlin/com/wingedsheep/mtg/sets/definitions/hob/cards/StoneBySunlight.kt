package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Stone by Sunlight — The Hobbit #27
 * {1}{W} · Instant · Uncommon
 *
 * Choose one —
 * • Destroy target creature with power 4 or greater.
 * • Until end of turn, target creature becomes an artifact in addition to its other types and
 *   gains indestructible.
 *
 * Modeling notes:
 *  - Each mode carries its own target, so the power-4 restriction only constrains mode 1; mode 2
 *    can hit any creature (including your own — the intended use, saving a creature from a wrath).
 *  - "Becomes an artifact **in addition to** its other types" is [Effects.AddCardType], not a
 *    become-artifact transform: the creature keeps its types, subtypes, abilities, and P/T. Both
 *    halves of the mode are `Duration.EndOfTurn`.
 */
val StoneBySunlight = card("Stone by Sunlight") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target creature with power 4 or greater.\n" +
        "• Until end of turn, target creature becomes an artifact in addition to its other types " +
        "and gains indestructible."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target creature with power 4 or greater") {
                val t = target("target", TargetCreature(filter = TargetFilter.Creature.powerAtLeast(4)))
                effect = Effects.Destroy(t)
            }
            mode("Target creature becomes an artifact and gains indestructible until end of turn") {
                val t = target("target", TargetCreature())
                effect = Effects.Composite(
                    Effects.AddCardType("ARTIFACT", t, Duration.EndOfTurn),
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t, Duration.EndOfTurn)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Kamila Szutenberg"
        flavorText = "\"Dawn take you all, and be stone to you!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5752731-253c-4b41-bdd8-94c26d715206.jpg?1784631953"
    }
}
