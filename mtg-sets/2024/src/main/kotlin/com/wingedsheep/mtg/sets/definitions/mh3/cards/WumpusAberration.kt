package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

val WumpusAberration = card("Wumpus Aberration") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Beast"
    power = 6
    toughness = 6
    oracleText = "Devoid (This card has no color.)\nWhen you cast this spell, if {C} wasn't spent to cast it, target opponent may put a creature card from their hand onto the battlefield.\nTrample"

    keywords(Keyword.DEVOID, Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.Not(Conditions.ManaSpentToCastIncludes(requiredColorless = 1))
        target = Targets.Opponent
        effect = Effects.ForEachPlayer(Player.TargetOpponent, Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature,
            prompt = "You may put a creature card from your hand onto the battlefield"
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Filip Burburan"
        flavorText = "All who behold its twisted form are consumed by a single-minded obsession: destroy it."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23c19f67-834c-4709-9038-7916ac0921eb.jpg?1783911254"
        ruling("2024-06-07", "Wumpus Aberration's triggered ability will resolve before Wumpus Aberration does. If Wumpus Aberration is countered or otherwise leaves the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
    }
}
