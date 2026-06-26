package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rydia's Return
 * {3}{G}{G}
 * Sorcery
 * Choose one —
 * • Creatures you control get +3/+3 until end of turn.
 * • Return up to two target permanent cards from your graveyard to your hand.
 */
val RydiasReturn = card("Rydia's Return") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Creatures you control get +3/+3 until end of turn.\n" +
        "• Return up to two target permanent cards from your graveyard to your hand."

    spell {
        modal(chooseCount = 1) {
            mode("Creatures you control get +3/+3 until end of turn") {
                effect = Effects.ForEachInGroup(
                    filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                    effect = ModifyStatsEffect(3, 3, EffectTarget.Self)
                )
            }
            mode("Return up to two target permanent cards from your graveyard to your hand") {
                target = TargetObject(
                    optional = true,
                    count = 2,
                    filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = ForEachTargetEffect(
                    effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Kohei Hayama"
        flavorText = "\"Are you okay? You should be able to move now.\"\n—Rydia, Summoner of Mist"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40a06165-2835-4610-86a1-7f684992fcf2.jpg?1748706502"
    }
}
