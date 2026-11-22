package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fortress Cyclops
 * {3}{R}{W}
 * Creature — Cyclops Soldier
 * 3/3
 * Whenever this creature attacks, it gets +3/+0 until end of turn.
 * Whenever this creature blocks, it gets +0/+3 until end of turn.
 */
val FortressCyclops = card("Fortress Cyclops") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Creature — Cyclops Soldier"
    oracleText = "Whenever this creature attacks, it gets +3/+0 until end of turn.\nWhenever this creature blocks, it gets +0/+3 until end of turn."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(3, 0, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.ModifyStats(0, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Maciej Kuciara"
        flavorText = "\"They think they tamed him, but he will always be a wild titan, a force of nature.\"\n—Nedja, Gruul shaman"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39f835f0-abba-402c-bdae-be03ad3fb658.jpg"
    }
}
