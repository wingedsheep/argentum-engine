package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Rats you control — the count gate and the pump target of [OgreChitterlord] read the same set. */
private val ratsYouControl = GameObjectFilter.Creature.withSubtype("Rat").youControl()

/**
 * "Create two Rats. Then if you control five or more Rats, each Rat you control gets +2/+0 until end
 * of turn."
 *
 * `.then` is load-bearing: the intervening "Then" means the five-Rat count and the pumped group are
 * both evaluated *after* the two tokens have entered, so the new Rats count toward the five and get
 * the bonus themselves ([TwistedSewerWitch] relies on the same snapshot-after-the-boundary rule).
 * This is an inline "if", not an intervening-'if' trigger condition — it's checked once during
 * resolution, not again when the ability would trigger.
 *
 * Shared by the enters and attacks halves of the printed ability, which the SDK models as two
 * triggered abilities (the [Triggers.EntersBattlefield] / [Triggers.Attacks] pair used for every
 * "enters or attacks" card).
 */
private fun ratSwarmAndRally(): Effect = woeRatToken(DynamicAmount.Fixed(2)).then(
    ConditionalEffect(
        condition = Conditions.YouControlAtLeast(5, ratsYouControl),
        effect = Patterns.Group.modifyStatsForAll(
            power = 2,
            toughness = 0,
            filter = GroupFilter(ratsYouControl)
        )
    )
)

/**
 * Ogre Chitterlord
 * {4}{R}{R}
 * Creature — Ogre Warrior
 * 6/5
 *
 * Menace
 * Whenever this creature enters or attacks, create two 1/1 black Rat creature tokens with "This
 * token can't block." Then if you control five or more Rats, each Rat you control gets +2/+0 until
 * end of turn.
 *
 * The Chitterlord is an Ogre, not a Rat, so it never pumps itself — but the two Rats it just made do
 * count toward the five and do get the +2/+0. Nothing targets, so hexproof Rats are included and the
 * ability can't fizzle.
 */
val OgreChitterlord = card("Ogre Chitterlord") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 6
    toughness = 5
    oracleText = "Menace\n" +
        "Whenever this creature enters or attacks, create two 1/1 black Rat creature tokens with " +
        "\"This token can't block.\" Then if you control five or more Rats, each Rat you control " +
        "gets +2/+0 until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ratSwarmAndRally()
        description = "Create two 1/1 black Rat creature tokens with \"This token can't block.\" " +
            "Then if you control five or more Rats, each Rat you control gets +2/+0 until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ratSwarmAndRally()
        description = "Create two 1/1 black Rat creature tokens with \"This token can't block.\" " +
            "Then if you control five or more Rats, each Rat you control gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "319"
        artist = "Piotr Foksowicz"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a70b2033-eec5-4c86-9ebe-a68960a86763.jpg?1783915039"
    }
}
