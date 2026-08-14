package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Riptide Gearhulk — Aetherdrift #219
 * {1}{W}{W}{U}{U} · Artifact Creature — Construct · 2/5
 *
 * Double strike
 * Prowess
 * When this creature enters, for each opponent, put up to one target nonland permanent that player
 * controls into its owner's library third from the top.
 *
 * "Third from the top" is a positional library move — [Effects.PutIntoLibraryNthFromTop] with
 * `positionFromTop = 2` (the parameter is 0-indexed: 0 = top, 2 = third). It goes to the *owner's*
 * library, which the move already does, so a permanent an opponent stole still lands in its owner's
 * deck. A library with fewer than two cards under the top simply takes it as deep as it goes.
 *
 * "For each opponent, … up to one target … that player controls" follows the corpus convention for
 * this wording (Blatant Thievery, Omega, Heartless Evolution): one *optional* target — exactly right
 * in 1v1, and the shape the multiplayer per-opponent targeting work generalizes. `optional = true`
 * carries the "up to one", so declining is legal and the trigger still resolves.
 */
val RiptideGearhulk = card("Riptide Gearhulk") {
    manaCost = "{1}{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 5
    oracleText = "Double strike\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature enters, for each opponent, put up to one target nonland permanent that " +
        "player controls into its owner's library third from the top."

    keywords(Keyword.DOUBLE_STRIKE, Keyword.PROWESS)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target(
            "target nonland permanent that player controls",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanentOpponentControls)
        )
        effect = Effects.PutIntoLibraryNthFromTop(victim, positionFromTop = 2)
        description = "When this creature enters, for each opponent, put up to one target nonland " +
            "permanent that player controls into its owner's library third from the top."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44bfb0f7-18ca-4f6e-ba64-92120010456e.jpg?1783907853"
    }
}
