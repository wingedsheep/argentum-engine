package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.dsl.GroupPatterns
import com.wingedsheep.sdk.dsl.Effects

/**
 * Wail of War
 * {2}{B}
 * Instant
 *
 * Choose one —
 * • Creatures target opponent controls get -1/-1 until end of turn.
 * • Return up to two target creature cards from your graveyard to your hand.
 *
 * Mode 1 scopes the group debuff to the chosen opponent via
 * [GameObjectFilter.Creature.targetOpponentControls] + the standard
 * [GroupPatterns.modifyStatsForAll]; mode 2 reuses the up-to-two graveyard-return
 * shape (see Dutiful Return).
 */
val WailOfWar = card("Wail of War") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Creatures target opponent controls get -1/-1 until end of turn.\n" +
        "• Return up to two target creature cards from your graveyard to your hand."

    spell {
        modal(chooseCount = 1) {
            mode("Creatures target opponent controls get -1/-1 until end of turn") {
                val opponent = target("target opponent", TargetOpponent())
                effect = GroupPatterns.modifyStatsForAll(
                    power = -1,
                    toughness = -1,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent))
                )
            }
            mode("Return up to two target creature cards from your graveyard to your hand") {
                target = TargetObject(
                    count = 2,
                    optional = true,
                    filter = TargetFilter.CreatureInYourGraveyard
                )
                effect = ForEachTargetEffect(
                    effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Izzy"
        flavorText = "Mardu war shrieks ensure their enemies are defeated before the battle even starts."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e9430dd-f583-400d-808a-64e2b5fa54f1.jpg?1743204354"
    }
}
