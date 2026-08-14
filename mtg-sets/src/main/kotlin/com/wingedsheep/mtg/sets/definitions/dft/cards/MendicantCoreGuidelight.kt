package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Mendicant Core, Guidelight — Aetherdrift #213. */
val MendicantCoreGuidelight = card("Mendicant Core, Guidelight") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Artifact Creature — Robot"
    oracleText = "Mendicant Core's power is equal to the number of artifacts you control.\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — Whenever you cast an artifact spell, you may pay {1}. If you do, copy it. " +
        "(The copy becomes a token.)"
    toughness = 3

    dynamicPower(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Artifact))
    startYourEngines()
    maxSpeed {
        triggeredAbility {
            trigger = Triggers.youCastSpell(GameObjectFilter.Artifact)
            effect = MayPayManaEffect(
                cost = ManaCost.parse("{1}"),
                effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f434b103-490f-424e-a0a1-efb1b931c8e6.jpg?1783907856"
        ruling("2025-02-07", "A resolving copy of a permanent spell becomes a token, so the token isn't created. Effects that care about a token being created won't interact with a token that enters the battlefield from Mendicant Core's last ability.")
        ruling("2025-02-07", "The copy remembers any decisions that were made for the original spell as it was cast, including values chosen for X in its mana cost and whether any alternative or additional costs were chosen.")
    }
}
