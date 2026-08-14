package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Three Blind Mice
 * {2}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Create a 1/1 white Mouse creature token.
 * II, III — Create a token that's a copy of target token you control.
 * IV — Creatures you control get +1/+1 and gain vigilance until end of turn.
 *
 * "II, III" is one printed line for two identical chapter abilities, so it's two `sagaChapter`
 * blocks: each triggers on its own lore counter and each picks its own target when it triggers.
 * Chapter II normally copies the Mouse from chapter I, but the target is any token you control — the
 * Saga doesn't remember what it made.
 *
 * Copying a token copies the *original* characteristics the token-creating effect specified, not the
 * token's current state (CR 707.2), which is why this is [Effects.CreateTokenCopyOfTarget] on a
 * permanent target rather than a fresh 1/1 Mouse: pointing chapter III at a copy of something bigger
 * duplicates that instead.
 *
 * Chapter IV is a snapshot: only creatures you control as it resolves get the bonus, so a Mouse
 * created later in the turn misses out.
 */
val ThreeBlindMice = card("Three Blind Mice") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice " +
        "after IV.)\n" +
        "I — Create a 1/1 white Mouse creature token.\n" +
        "II, III — Create a token that's a copy of target token you control.\n" +
        "IV — Creatures you control get +1/+1 and gain vigilance until end of turn."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Mouse"),
            imageUri = "https://cards.scryfall.io/normal/front/d/8/d80435c8-9a41-47cb-be84-784a278adcae.jpg?1783914992",
        )
    }

    sagaChapter(2) {
        val token = target(
            "target token you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.token().youControl())),
        )
        effect = Effects.CreateTokenCopyOfTarget(token)
    }

    sagaChapter(3) {
        val token = target(
            "target token you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.token().youControl())),
        )
        effect = Effects.CreateTokenCopyOfTarget(token)
    }

    sagaChapter(4) {
        effect = Effects.Composite(
            Patterns.Group.modifyStatsForAll(
                power = 1,
                toughness = 1,
                filter = Filters.Group.creaturesYouControl,
            ),
            Patterns.Group.grantKeywordToAll(
                Keyword.VIGILANCE,
                Filters.Group.creaturesYouControl,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d2ba371-854c-4529-b72b-e4a1887e33ab.jpg?1783915125"

        ruling(
            "2023-09-01",
            "The token copies the original characteristics of the target token as stated by the " +
                "effect that created the token."
        )
        ruling(
            "2023-09-01",
            "If the copied token is copying something else, then the token enters the battlefield " +
                "as whatever that token copied."
        )
        ruling(
            "2023-09-01",
            "Only creatures you control at the time the fourth chapter ability resolves will get " +
                "+1/+1 and gain vigilance. Creatures you begin to control later in the turn won't " +
                "be affected."
        )
    }
}
