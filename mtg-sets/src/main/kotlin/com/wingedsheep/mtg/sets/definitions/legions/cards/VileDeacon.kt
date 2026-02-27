package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Vile Deacon
 * {2}{B}{B}
 * Creature — Human Cleric
 * 2/2
 * Whenever Vile Deacon attacks, it gets +X/+X until end of turn, where X is the number of Clerics on the battlefield.
 */
val VileDeacon = card("Vile Deacon") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever Vile Deacon attacks, it gets +X/+X until end of turn, where X is the number of Clerics on the battlefield."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(
            power = DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Cleric")),
            toughness = DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Cleric")),
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Matthew D. Wilson"
        flavorText = "\"The Cabal and the Order really aren't that different. After all, we are both empowered by faith.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2641bd5-c845-47a1-8038-bb28b06f896e.jpg?1562930921"
    }
}
