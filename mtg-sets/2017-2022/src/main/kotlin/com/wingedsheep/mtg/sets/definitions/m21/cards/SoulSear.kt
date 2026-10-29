package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Soul Sear — Core Set 2021 #160 (canonical printing)
 * {2}{R} · Instant
 *
 * Soul Sear deals 5 damage to target creature or planeswalker. That permanent loses indestructible
 * until end of turn.
 *
 * Smite the Deathless's shape: the damage is marked first, the indestructible removal follows in
 * the same resolution, and state-based actions only check once the spell has finished resolving.
 */
val SoulSear = card("Soul Sear") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Soul Sear deals 5 damage to target creature or planeswalker. That permanent loses " +
        "indestructible until end of turn."

    spell {
        val permanent = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(5, permanent) then
            Effects.RemoveKeyword(Keyword.INDESTRUCTIBLE, permanent, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Mathias Kollros"
        flavorText = "There was no witty retort, no confident boast to accompany the attack. There was only pain."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62584e4f-dac1-4d99-ac0a-6a2451603889.jpg?1783930684"
        ruling(
            "2020-06-23",
            "Soul Sear can target a creature or planeswalker that doesn't have indestructible."
        )
    }
}
