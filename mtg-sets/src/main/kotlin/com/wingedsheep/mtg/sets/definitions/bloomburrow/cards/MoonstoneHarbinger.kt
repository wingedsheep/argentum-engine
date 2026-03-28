package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Moonstone Harbinger
 * {2}{B}
 * Creature — Bat Warrior
 * 1/3
 *
 * Flying, deathtouch
 * Whenever you gain or lose life during your turn, Bats you control get +1/+0
 * and gain deathtouch until end of turn. This ability triggers only once each turn.
 */
val MoonstoneHarbinger = card("Moonstone Harbinger") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Bat Warrior"
    power = 1
    toughness = 3
    oracleText = "Flying, deathtouch\n" +
        "Whenever you gain or lose life during your turn, Bats you control get +1/+0 " +
        "and gain deathtouch until end of turn. This ability triggers only once each turn."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    // Whenever you gain or lose life during your turn, Bats you control get +1/+0
    // and gain deathtouch until end of turn. This ability triggers only once each turn.
    triggeredAbility {
        trigger = Triggers.YouGainOrLoseLife
        triggerCondition = Conditions.IsYourTurn
        oncePerTurn = true
        val batsYouControl = GroupFilter.allCreaturesWithSubtype("Bat").youControl()
        effect = CompositeEffect(
            listOf(
                EffectPatterns.modifyStatsForAll(1, 0, batsYouControl),
                EffectPatterns.grantKeywordToAll(Keyword.DEATHTOUCH, batsYouControl)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Kevin Sidharta"
        flavorText = "Moonstone weapons drink in the last light of dusk to give their wielders an edge in combat."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59e4aa8d-1d06-48db-b205-aa2f1392bbcb.jpg?1721426452"
    }
}
