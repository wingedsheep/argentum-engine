package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rubblebelt Rioters
 * {1}{R}{G}
 * Creature — Human Berserker
 * 0/4
 * Haste
 * Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the greatest power among creatures you control.
 *
 * X is read on resolution and includes this creature's own power.
 */
val RubblebeltRioters = card("Rubblebelt Rioters") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Berserker"
    power = 0
    toughness = 4
    oracleText = "Haste\n" +
        "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the greatest power among creatures you control."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Tomasz Jedruszek"
        flavorText = "\"If the system is broken, break the system.\"\n—Domri Rade"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bf8df62-eca1-4a45-84af-fd7c88245a42.jpg?1783933388"
    }
}
