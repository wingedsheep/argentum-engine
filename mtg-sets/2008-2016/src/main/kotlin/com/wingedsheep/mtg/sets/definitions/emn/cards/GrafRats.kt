package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Graf Rats
 * {1}{B}
 * Creature — Rat
 * 2/1
 * At the beginning of combat on your turn, if you both own and control this creature and a
 * creature named Midnight Scavengers, exile them, then meld them into Chittering Host.
 *
 * The whole intervening "if" — own and control both — is the trigger condition, so a stolen
 * piece doesn't trigger at all; [Effects.Meld] checks it again as it resolves.
 */
val GrafRats = card("Graf Rats") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    oracleText = "At the beginning of combat on your turn, if you both own and control this " +
        "creature and a creature named Midnight Scavengers, exile them, then meld them into " +
        "Chittering Host."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.All(
            Conditions.SourceMatches(GameObjectFilter.Any.ownedByYou().youControl()),
            Conditions.YouControl(Filters.Creature.named("Midnight Scavengers").ownedByYou())
        )
        effect = Effects.Meld(Filters.Creature.named("Midnight Scavengers"), into = "Chittering Host")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3dedaff6-bd69-4fe3-a301-f7ea7c2f2861.jpg?1783937482"
    }
}
