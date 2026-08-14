package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sanguine Savior — Murders at Karlov Manor #230
 * {1}{W}{B} · Creature — Vampire Cleric · 2/1
 *
 * Flying, lifelink
 * Disguise {W/B}{W/B}
 * When this creature is turned face up, another target creature you control gains lifelink until
 * end of turn.
 *
 * Unlike its Disguise neighbours that read "enters **or** is turned face up", this trigger is
 * face-up only ([Triggers.TurnedFaceUp], SELF). Hard-casting the Savior for {1}{W}{B} gets you a
 * flying lifelinker and nothing else; the lifelink hand-out is the reward for going the long way
 * round, and CR 702.168d — turning face up is not entering the battlefield — is what keeps the two
 * routes apart with no extra wiring.
 *
 * "**Another** target creature you control" is [Targets.OtherCreatureYouControl], which excludes
 * the Savior itself. That exclusion has teeth here: the Savior already has printed lifelink, so
 * without it the ability could be pointed at a creature that gains nothing, and — worse — a lone
 * Savior with no other creature would still be offered a legal target. With the exclusion it simply
 * has no legal target in that spot, and the ability is removed from the stack.
 */
val SanguineSavior = card("Sanguine Savior") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire Cleric"
    power = 2
    toughness = 1
    oracleText = "Flying, lifelink\n" +
        "Disguise {W/B}{W/B} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, another target creature you control gains " +
        "lifelink until end of turn."

    keywords(Keyword.FLYING, Keyword.LIFELINK)
    disguise = "{W/B}{W/B}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val other = target("another target creature you control", Targets.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.LIFELINK, other)
        description = "When this creature is turned face up, another target creature you control " +
            "gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "230"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cba5503-ba99-43d8-8062-66d905e0d86b.jpg?1783912837"
    }
}
