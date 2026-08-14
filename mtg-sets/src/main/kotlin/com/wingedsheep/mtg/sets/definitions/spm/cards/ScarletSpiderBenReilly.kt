package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scarlet Spider, Ben Reilly — Marvel's Spider-Man #142
 * {1}{R}{G} · Legendary Creature — Spider Human Hero · 4/3
 *
 * Web-slinging {R}{G}
 * Trample
 * Sensational Save — If Scarlet Spider was cast using web-slinging, he enters with X +1/+1 counters
 * on him, where X is the mana value of the returned creature.
 *
 * The "enters with X +1/+1 counters" clause reads the returned creature's mana value that the
 * web-slinging pipeline captured at cast time (CR 118.9c — the *returned* creature's mana value, not
 * Scarlet Spider's) and stamped onto the resolving permanent under
 * [ChoiceSlot.WEB_SLUNG_RETURNED_MV]. When Scarlet Spider is *not* web-slung the slot is absent, so
 * [DynamicAmount.CastChoice] reads 0 and he enters with no counters — exactly the "if … was cast
 * using web-slinging" gate. Modeled as an [EntersWithDynamicCounters] replacement (CR 614.1c) rather
 * than an ETB trigger so the counters are present the instant he enters.
 */
val ScarletSpiderBenReilly = card("Scarlet Spider, Ben Reilly") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 4
    toughness = 3
    oracleText = "Web-slinging {R}{G} (You may cast this spell for {R}{G} if you also return a " +
        "tapped creature you control to its owner's hand.)\n" +
        "Trample\n" +
        "Sensational Save — If Scarlet Spider was cast using web-slinging, he enters with X +1/+1 " +
        "counters on him, where X is the mana value of the returned creature."

    webSlinging("{R}{G}")
    keywords(Keyword.TRAMPLE)
    replacementEffect(
        EntersWithDynamicCounters(count = DynamicAmount.CastChoice(ChoiceSlot.WEB_SLUNG_RETURNED_MV))
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee771581-f867-48d7-9ddb-897a1ffcdf0a.jpg?1783905313"
    }
}
