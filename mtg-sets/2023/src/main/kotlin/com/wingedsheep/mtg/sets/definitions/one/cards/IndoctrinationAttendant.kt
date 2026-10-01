package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Indoctrination Attendant
 * {3}{W}
 * Creature — Phyrexian Cleric
 * 3/4
 * Toxic 1
 * When this creature enters, you may return another permanent you control to its owner's hand.
 * If you do, create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and
 * "This token can't block."
 *
 * The return is untargeted: the permanent is chosen on resolution. "You may" is an up-to-one
 * choice (choosing none declines), and the Mite is gated on the permanent actually having moved.
 */
val IndoctrinationAttendant = card("Indoctrination Attendant") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Cleric"
    power = 3
    toughness = 4
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When this creature enters, you may return another permanent you control to its owner's hand. " +
        "If you do, create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and " +
        "\"This token can't block.\""

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, you may return another permanent you control to " +
            "its owner's hand. If you do, create a 1/1 colorless Phyrexian Mite artifact creature " +
            "token with toxic 1 and \"This token can't block.\""
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Permanent,
                    player = Player.You,
                    excludeSelf = true
                )
            )
            val chosen = chooseUpTo(
                1,
                from = candidates,
                prompt = "You may return another permanent you control to its owner's hand",
                useTargetingUI = true
            )
            val returned = moveTracked(chosen, CardDestination.ToZone(Zone.HAND))
            run(Effects.If(
                condition = whenMatches(returned),
                then = Effects.CreatePhyrexianMite(1)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Sam Wolfe Connelly"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d051bfc7-b402-444d-896e-c3a9562cdb0d.jpg?1783918080"
    }
}
