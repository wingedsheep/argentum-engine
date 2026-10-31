package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressHexproofForGroup
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kaya, Bane of the Dead
 * {3}{W/B}{W/B}{W/B}
 * Legendary Planeswalker — Kaya
 * Loyalty: 7
 *
 * Your opponents and permanents your opponents control with hexproof can be the targets of spells
 * and abilities you control as though they didn't have hexproof.
 * −3: Exile target creature.
 *
 * The static is [SuppressHexproofForGroup] over every permanent an opponent controls, widened to
 * the opponents themselves with `players`; "with hexproof" needs no filter, since only hexproof is
 * being looked through.
 */
val KayaBaneOfTheDead = card("Kaya, Bane of the Dead") {
    manaCost = "{3}{W/B}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Kaya"
    startingLoyalty = 7
    oracleText = "Your opponents and permanents your opponents control with hexproof can be the targets of " +
        "spells and abilities you control as though they didn't have hexproof.\n" +
        "−3: Exile target creature."

    staticAbility {
        ability = SuppressHexproofForGroup(
            filter = GroupFilter(GameObjectFilter.Permanent.opponentControls()),
            players = Player.EachOpponent,
        )
    }

    loyaltyAbility(-3) {
        val t = target(TargetFilter.Creature)
        effect = Effects.Exile(t)
        description = "Exile target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9b575d3-f3f9-4ff4-8e5c-da7c2ec69882.jpg?1783933378"
        ruling("2019-05-03", "If a spell or ability you control targets an opponent with hexproof or an opponent's permanent with hexproof, and Kaya leaves the battlefield while that spell or ability is on the stack, that player or permanent becomes an illegal target for that spell or ability. This includes Kaya's own loyalty ability.")
    }
}
