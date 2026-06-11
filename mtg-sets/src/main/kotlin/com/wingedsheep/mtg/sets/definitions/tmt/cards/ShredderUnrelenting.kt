package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Shredder, Unrelenting
 * {4}{B}
 * Legendary Creature — Human Ninja
 * 6/4
 *
 * Sneak {3}{B} (You may cast this spell for {3}{B} if you also return an
 * unblocked attacker you control to hand during the declare blockers step. He
 * enters tapped and attacking.)
 * Deathtouch
 * Whenever Shredder enters or attacks, another target creature you control
 * gains deathtouch until end of turn.
 */
val ShredderUnrelenting = card("Shredder, Unrelenting") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Ninja"
    oracleText = "Sneak {3}{B} (You may cast this spell for {3}{B} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nDeathtouch\nWhenever Shredder enters or attacks, another target creature you control gains deathtouch until end of turn."
    power = 6
    toughness = 4

    sneak("{3}{B}")
    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("another target creature you control", Targets.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, t, duration = Duration.EndOfTurn)
    }
    triggeredAbility {
        trigger = Triggers.Attacks
        val t = target("another target creature you control", Targets.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, t, duration = Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88a48867-5c65-483c-92b0-f70c53ea2a9e.jpg?1771502616"
    }
}
