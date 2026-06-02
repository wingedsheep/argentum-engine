package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Skirmish Rhino
 * {W}{B}{G}
 * Creature — Rhino
 * 3/4
 *
 * Trample
 * When this creature enters, each opponent loses 2 life and you gain 2 life.
 */
val SkirmishRhino = card("Skirmish Rhino") {
    manaCost = "{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Creature — Rhino"
    power = 3
    toughness = 4
    oracleText = "Trample\nWhen this creature enters, each opponent loses 2 life and you gain 2 life."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                LoseLifeEffect(DynamicAmount.Fixed(2), EffectTarget.PlayerRef(Player.EachOpponent)),
                GainLifeEffect(DynamicAmount.Fixed(2), EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "James Bousema"
        flavorText = "Slowly, the clans have begun to recover and adapt long-forgotten tactics."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a2e9ba1-c254-41e3-9845-4e81f9fec38d.jpg?1743204887"
    }
}
