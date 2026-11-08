package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abyssal Gatekeeper — Weatherlight #59 (canonical printing)
 * {1}{B} · Creature — Horror · 1/1
 *
 * When this creature dies, each player sacrifices a creature of their choice.
 *
 * "Each player sacrifices" names the players, so it is [Effects.Sacrifice]'s target form over
 * `Player.Each` (Slum Reaper's shape). The Gatekeeper is already in the graveyard when the trigger
 * resolves, so its own controller sacrifices from whatever else they control.
 */
val AbyssalGatekeeper = card("Abyssal Gatekeeper") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, each player sacrifices a creature of their choice."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Mark Tedin"
        flavorText = "\"There are two ways for me to pass this gate. One involves you remaining conscious.\"\n—Gerrard of the *Weatherlight*"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1734df5a-7d3a-46c7-a0ad-adbbd1be958f.jpg?1783946738"
    }
}
