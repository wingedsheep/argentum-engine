package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * D'Ghor Bloodletter
 * {2}{B}
 * Creature — Klingon Assassin
 * 2/2
 *
 * When this creature enters, choose one —
 * • All other creatures get -1/-1 until end of turn.
 * • Each player sacrifices a creature of their choice.
 */
val DGhorBloodletter = card("D'Ghor Bloodletter") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Klingon Assassin"
    colorIdentity = "B"
    oracleText = "When this creature enters, choose one —\n• All other creatures get -1/-1 until end of turn.\n• Each player sacrifices a creature of their choice."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(listOf(
            mode("All other creatures get -1/-1 until end of turn") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature, excludeSelf = true),
                    Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity)
                )
            },
            mode("Each player sacrifices a creature") {
                effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each))
            }
        ))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Patricia Pria"
        flavorText = "\"I am eager to know if his blood runs as readily as his ship.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5075645d-ef69-4559-953e-c3140153bd75.jpg?1792753733"
    }
}
