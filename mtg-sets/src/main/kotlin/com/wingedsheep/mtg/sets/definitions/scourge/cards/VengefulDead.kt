package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.scripting.OnOtherCreatureWithSubtypeDies

/**
 * Vengeful Dead
 * {3}{B}
 * Creature — Zombie
 * 3/2
 * Whenever Vengeful Dead or another Zombie dies, each opponent loses 1 life.
 */
val VengefulDead = card("Vengeful Dead") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 2
    oracleText = "Whenever Vengeful Dead or another Zombie dies, each opponent loses 1 life."

    // When Vengeful Dead itself dies
    triggeredAbility {
        trigger = Triggers.Dies
        effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // When another Zombie dies (any controller)
    triggeredAbility {
        trigger = OnOtherCreatureWithSubtypeDies(
            subtype = Subtype("Zombie"),
            youControlOnly = false
        )
        effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Alex Horley-Orlandelli"
        flavorText = "Those who don't learn from their deaths are destined to repeat them."
        imageUri = "https://cards.scryfall.io/normal/front/7/cNice/7c11c11d-9809-4031-8cbc-21aef07d7f1f.jpg?1562531178"
    }
}
