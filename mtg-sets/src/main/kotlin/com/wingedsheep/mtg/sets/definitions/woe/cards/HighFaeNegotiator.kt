package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * High Fae Negotiator
 * {3}{B}{B}
 * Creature — Faerie Warlock
 * 3/5
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Flying
 * When this creature enters, if it was bargained, each opponent loses 3 life and you gain 3 life.
 *
 * The permanent shape of bargain (CR 702.166b), same as [TroublemakerOuphe]: the bargained fact is
 * stamped on the spell and rides the permanent it becomes, so the enters trigger can still read
 * it. Modelled as an intervening-'if' clause (CR 603.4) on [Conditions.WasBargained] — an
 * unbargained cast never puts the ability on the stack at all.
 *
 * The drain is *not* [Effects.DrainLife]: the life gain is a flat 3, not "life equal to the life
 * lost this way", so with two or more opponents the Negotiator still gains exactly 3. That's a
 * [Effects.LoseLife] over [Player.EachOpponent] composed with a fixed [Effects.GainLife], the same
 * shape Sinister Monolith uses.
 */
val HighFaeNegotiator = card("High Fae Negotiator") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Faerie Warlock"
    power = 3
    toughness = 5
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Flying\n" +
        "When this creature enters, if it was bargained, each opponent loses 3 life and you gain " +
        "3 life."

    bargain()

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasBargained
        effect = Effects.Composite(
            Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(3),
        )
        description = "When this creature enters, if it was bargained, each opponent loses 3 life " +
            "and you gain 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Anna Christenson"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0fc77e7-154d-4433-93b3-1a1dee34791b.jpg?1783915107"

        ruling(
            "2023-09-01",
            "You can bargain a permanent spell even if you won't be able to choose targets for an " +
                "enters-the-battlefield ability of that permanent once the spell resolves."
        )
        ruling(
            "2023-09-01",
            "If a card or token enters the battlefield as a copy of a permanent that's already on the " +
                "battlefield, the new permanent isn't bargained, even if the original was."
        )
    }
}
