package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pillar of Flame
 * {R}
 * Sorcery
 * Pillar of Flame deals 2 damage to any target. If a creature dealt damage this way would die this
 * turn, exile it instead.
 *
 * Yamabushi's Flame's shape: the [Effects.MarkExileOnDeath] mark goes on before the damage so a
 * creature killed by this very spell is exiled. Marking a player is a no-op in the executor.
 */
val PillarOfFlame = card("Pillar of Flame") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Pillar of Flame deals 2 damage to any target. If a creature dealt damage this way " +
        "would die this turn, exile it instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.MarkExileOnDeath(t) then Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Karl Kopinski"
        flavorText = "\"May the worthy spend an eternity in Blessed Sleep. May the wicked find the peace of oblivion.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c983e879-d9d2-47cc-9958-506711ca80cd.jpg?1783940678"
    }
}
