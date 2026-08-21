package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul of Migration — Modern Horizons 2 #33
 * {5}{W}{W} · Creature — Elemental · 2 / 4
 *
 * Flying
 * When this creature enters, create two 1/1 white Bird creature tokens with flying.
 * Evoke {3}{W} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)
 *
 * The token half is one [Effects.CreateToken] with `count = 2`, not two effects: "create two … "
 * is a single creation event, so a "whenever one or more creatures enter" watcher sees one batch.
 * No `imageUri` is passed — the Bird token's art resolves from Modern Horizons 2's own token sheet
 * via `TokenArtData`, and hard-coding a URL here would pin the card to some other set's printing.
 *
 * `evoke` is the first-class alternative-cost field on the card DSL (CR 702.74a); the engine offers
 * the cheaper cast and supplies the "when this permanent enters, if its evoke cost was paid,
 * sacrifice it" trigger itself, so nothing else is authored for it. Both enters triggers go on the
 * stack together and their controller orders them; the Birds arrive either way, which is the whole
 * point of the card — {3}{W} for two flying bodies.
 */
val SoulOfMigration = card("Soul of Migration") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, create two 1/1 white Bird creature tokens with flying.\n" +
        "Evoke {3}{W} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)"

    keywords(Keyword.FLYING)

    evoke = "{3}{W}"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            count = 2
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4541217f-5e86-491b-918b-ed7a2eb3e4eb.jpg?1783926884"
    }
}
