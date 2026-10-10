package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lazotep Plating — War of the Spark #59 (canonical printing)
 * {1}{U}
 * Instant
 * Amass Zombies 1.
 * You and permanents you control gain hexproof until end of turn.
 *
 * The amass runs first, so a Zombie Army token it creates is already among "permanents you control"
 * when the group grant gathers its members — the 2019-05-03 ruling. The player half is the
 * player-hexproof grant (`Effects.GrantHexproof` on the controller), the same pairing Dawn's Truce uses.
 */
val LazotepPlating = card("Lazotep Plating") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Amass Zombies 1. (Put a +1/+1 counter on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)\n" +
        "You and permanents you control gain hexproof until end of turn. " +
        "(You and they can't be the targets of spells or abilities your opponents control.)"

    spell {
        effect = Effects.Amass(1, "Zombie") then
            Effects.GrantHexproof(EffectTarget.Controller) then
            Patterns.Group.grantKeywordToAll(Keyword.HEXPROOF, Filters.Group.permanentsYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f03b5405-6016-405b-a504-a454731b9276.jpg?1783933460"

        ruling("2019-05-03", "You amass 1 and grant hexproof all while Lazotep Plating is resolving. Nothing can happen between the two, and no player may choose to take actions.")
        ruling("2019-05-03", "If you create a Zombie Army token when Lazotep Plating instructs you to amass 1, that token will gain hexproof until end of turn.")
    }
}
