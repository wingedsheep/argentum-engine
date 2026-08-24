package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rockslide Ambush
 * {1}{R}
 * Sorcery
 *
 * Spitting Earth's shape: the damage amount is a battlefield tally
 * ([DynamicAmount.AggregateBattlefield]) over the lands you control carrying the Mountain subtype.
 */
val RockslideAmbush = card("Rockslide Ambush") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Rockslide Ambush deals damage to target creature equal to the number of Mountains you control."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.DealDamage(
            DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)
            ),
            t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Inoue Junichi"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04e5faf1-25c9-46c0-88f2-c59e7b9c08c5.jpg"
    }
}
