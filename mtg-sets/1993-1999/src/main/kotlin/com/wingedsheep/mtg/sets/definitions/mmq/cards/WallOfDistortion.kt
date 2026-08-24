package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Wall of Distortion
 * {2}{B}{B}
 * Creature — Wall
 * 1 / 3
 *
 * "Activate only as a sorcery" is a *timing rule*, not an [com.wingedsheep.sdk.scripting.ActivationRestriction]:
 * `ActivationRestriction` has no sorcery member, and `OnlyDuringYourTurn` would still permit
 * instant speed on your own turn. `TimingRule.SorcerySpeed` is the one that says main phase,
 * your turn, empty stack (`rules-engine/.../core/TurnManager.kt`).
 *
 * The discard itself is [Patterns.Hand]'s gather → select → move pipeline with the *target
 * player* as both the hand's owner and the chooser.
 */
val WallOfDistortion = card("Wall of Distortion") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wall"
    oracleText = "Defender (This creature can't attack.)\n" +
        "{2}{B}, {T}: Target player discards a card. Activate only as a sorcery."
    power = 1
    toughness = 3

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        val t = target("target", Targets.Player)
        effect = Patterns.Hand.discardCards(1, t)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Mark Tedin"
        flavorText = "It reflects only nightmares."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2b2d07a-9ea1-430d-b432-ae507f4fe73b.jpg"
    }
}
