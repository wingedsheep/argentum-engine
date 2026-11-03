package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Obsidian Order Operative
 * {3}{B}
 * Creature — Cardassian Officer Spy
 * 2/5
 *
 * Whenever you face a dilemma, each opponent loses 1 life and you gain 1 life. (You face a dilemma
 * as you choose one or more modes for a spell or ability.)
 */
val ObsidianOrderOperative = card("Obsidian Order Operative") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Cardassian Officer Spy"
    colorIdentity = "B"
    oracleText = "Whenever you face a dilemma, each opponent loses 1 life and you gain 1 life. " +
        "(You face a dilemma as you choose one or more modes for a spell or ability.)"
    power = 2
    toughness = 5
    triggeredAbility {
        trigger = Triggers.you.facesADilemma()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Wero Gallo"
        flavorText = "\"Your resistance is wasted energy, and in time you'll wish you hadn't wasted it.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17a7b0a8-baaf-4e6a-8536-f511c796a713.jpg?1793361289"
    }
}
