package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psionic Blast
 * {2}{U}
 * Instant
 *
 * Psionic Blast deals 4 damage to any target and 2 damage to you.
 *
 * The self-damage is untargeted; if the single target becomes illegal the whole spell
 * doesn't resolve, so no self-damage either.
 */
val PsionicBlast = card("Psionic Blast") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Psionic Blast deals 4 damage to any target and 2 damage to you."

    spell {
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(4, victim) then Effects.DealDamage(2, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6a86e6e-bfff-46af-9d36-c912901fea92.jpg?1783948702"
    }
}
