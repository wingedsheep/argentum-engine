package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agate-Blade Assassin
 * {1}{B}
 * Creature — Lizard Assassin
 * 1/3
 *
 * Whenever this creature attacks, defending player loses 1 life and you gain 1 life.
 */
val AgateBladeAssassin = card("Agate-Blade Assassin") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Lizard Assassin"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature attacks, defending player loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                LoseLifeEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                GainLifeEffect(1, EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Hristo D. Chukov"
        flavorText = "Some lizardfolk wield the precious agate and yearn for fire. Others wield it and thirst for blood."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39ebb84a-1c52-4b07-9bd0-b360523b3a5b.jpg?1721426332"
    }
}
