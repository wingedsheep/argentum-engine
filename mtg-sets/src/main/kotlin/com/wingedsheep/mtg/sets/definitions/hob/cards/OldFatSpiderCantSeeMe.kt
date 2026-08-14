package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Old Fat Spider Can't See Me — The Hobbit #50
 * {2}{U} · Enchantment — Saga · Uncommon
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Target creature you control gains hexproof for as long as this Saga remains on the battlefield.
 * II — Prevent all damage that would be dealt by up to one target creature for as long as this Saga
 *      remains on the battlefield.
 * III, IV — Draw a card.
 *
 * Both open-ended chapters use [Duration.WhileSourceOnBattlefield], whose source is the Saga: the
 * hexproof grant and the damage shield are ordinary floating effects gated on the Saga still being
 * on the battlefield, so they end the moment it leaves — including the sacrifice that follows
 * chapter IV (CR 714.4), which is why chapters III and IV only draw. CR 611.2b makes the gate a
 * one-way latch: a blinked Saga is a new object and does not resume the old effects.
 *
 * Chapter II's "up to one target" is `optional = true`, so it may be cast with no target at all;
 * declining, or the chosen creature leaving before the chapter resolves, simply creates no shield.
 * The shield is on the *source* of damage, not the recipient — the creature deals no damage to
 * anything, combat or otherwise.
 */
val OldFatSpiderCantSeeMe = card("Old Fat Spider Can't See Me") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Target creature you control gains hexproof for as long as this Saga remains on the battlefield.\n" +
        "II — Prevent all damage that would be dealt by up to one target creature for as long as " +
        "this Saga remains on the battlefield.\n" +
        "III, IV — Draw a card."

    // I — Target creature you control gains hexproof for as long as this Saga remains on the
    //     battlefield.
    sagaChapter(1) {
        val creature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.GrantKeyword(
            Keyword.HEXPROOF,
            creature,
            Duration.WhileSourceOnBattlefield("this Saga")
        )
    }

    // II — Prevent all damage that would be dealt by up to one target creature for as long as this
    //      Saga remains on the battlefield.
    sagaChapter(2) {
        val creature = target(
            "up to one target creature",
            TargetCreature(optional = true)
        )
        effect = Effects.PreventAllDamageDealtBy(
            creature,
            Duration.WhileSourceOnBattlefield("this Saga")
        )
    }

    // III, IV — Draw a card.
    sagaChapter(3) { effect = Effects.DrawCards(1) }
    sagaChapter(4) { effect = Effects.DrawCards(1) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a865cea-f947-4736-8ace-ba478fceeb22.jpg?1785497065"
    }
}
