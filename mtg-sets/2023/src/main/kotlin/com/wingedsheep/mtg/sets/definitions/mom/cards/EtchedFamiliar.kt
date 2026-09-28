package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Etched Familiar (March of the Machine)
 * {2}{B} Artifact Creature — Phyrexian Fox 3/2
 * When this creature dies, each opponent loses 2 life and you gain 2 life.
 */
val EtchedFamiliar: CardDefinition = card("Etched Familiar") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Phyrexian Fox"
    power = 3
    toughness = 2
    oracleText = "When this creature dies, each opponent loses 2 life and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(2)
        description = "When this creature dies, each opponent loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Colin Boyer"
        flavorText = "\"No refunds.\"\n—Chammi, curio vendor"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4616a548-269b-4530-97e8-c690ccc138f3.jpg?1783917013"
    }
}
