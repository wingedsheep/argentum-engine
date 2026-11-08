package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Incendiary Flow (Eldritch Moon #133)
 * {1}{R}
 * Sorcery
 * Incendiary Flow deals 3 damage to any target. If a creature dealt damage this way would die
 * this turn, exile it instead.
 *
 * Yamabushi's Flame's shape at sorcery speed: the [Effects.MarkExileOnDeath] mark goes on before
 * the damage so a creature killed by this very spell is exiled. Marking a player is a no-op.
 */
val IncendiaryFlow = card("Incendiary Flow") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Incendiary Flow deals 3 damage to any target. If a creature dealt damage this way " +
        "would die this turn, exile it instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.MarkExileOnDeath(t) then Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Raymond Swanland"
        flavorText = "\"What's your plan?\" Gideon asked.\n\"Are you serious?\" Chandra replied."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf464f61-8a7f-493b-a80f-2f2b0ebd8bf6.jpg?1783937459"
    }
}
