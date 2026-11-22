package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clan Defiance
 * {X}{R}{G}
 * Sorcery
 * Choose one or more —
 * • Clan Defiance deals X damage to target creature with flying.
 * • Clan Defiance deals X damage to target creature without flying.
 * • Clan Defiance deals X damage to target player or planeswalker.
 *
 * Collective Defiance's "choose one or more" modal shape, without escalate: every mode deals the
 * same X.
 */
val ClanDefiance = card("Clan Defiance") {
    manaCost = "{X}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Choose one or more —\n" +
        "• Clan Defiance deals X damage to target creature with flying.\n" +
        "• Clan Defiance deals X damage to target creature without flying.\n" +
        "• Clan Defiance deals X damage to target player or planeswalker."

    spell {
        modal(chooseCount = 3, minChooseCount = 1) {
            mode("Clan Defiance deals X damage to target creature with flying.") {
                val flier = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
                effect = Effects.DealDamage(DynamicAmounts.xValue(), flier)
            }
            mode("Clan Defiance deals X damage to target creature without flying.") {
                val groundCreature = target(TargetFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING)))
                effect = Effects.DealDamage(DynamicAmounts.xValue(), groundCreature)
            }
            mode("Clan Defiance deals X damage to target player or planeswalker.") {
                val playerOrPlaneswalker = target(Targets.PlayerOrPlaneswalker)
                effect = Effects.DealDamage(DynamicAmounts.xValue(), playerOrPlaneswalker)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efa05298-9c94-4179-b75a-49ee2ca92920.jpg"
    }
}
