package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CopyEntryCounters
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Moritte of the Frost — Kaldheim #223
 * {2}{G}{U}{U} · Legendary Snow Creature — Shapeshifter · Uncommon
 * 0/0
 *
 * Changeling. You may have Moritte enter as a copy of a permanent you control, except it's legendary
 * and snow in addition to its other types and, if it's a creature, it enters with two additional
 * +1/+1 counters on it and has changeling.
 *
 * Both "if it's a creature" halves ask about the copy, not the copied permanent: the changeling is a
 * [CopyExceptions.conditional] clause tested on the copy's copiable type line, the counters an
 * `ifEntrantMatches` rider. Copying a land animated by a non-copy effect gives a noncreature Moritte
 * with neither (ruling).
 */
val MoritteOfTheFrost = card("Moritte of the Frost") {
    manaCost = "{2}{G}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Snow Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "Changeling (This card is every creature type.)\n" +
        "You may have Moritte enter as a copy of a permanent you control, except it's legendary and snow in " +
        "addition to its other types and, if it's a creature, it enters with two additional +1/+1 counters on " +
        "it and has changeling."

    keywords(Keyword.CHANGELING)

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Permanent.youControl(),
            exceptions = CopyExceptions(
                addedSupertypes = setOf(Supertype.LEGENDARY, Supertype.SNOW),
                conditional = listOf(
                    CopyExceptions.IfCardType(
                        CardType.CREATURE,
                        CopyExceptions(addedKeywords = setOf(Keyword.CHANGELING)),
                    )
                ),
            ),
            entersWithCounters = listOf(CopyEntryCounters(
                count = DynamicAmounts.fixed(2),
                ifEntrantMatches = GameObjectFilter.Creature,
            )),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22fdbe71-abca-4e48-99b2-1cb6b35d930b.jpg?1783928193"
        ruling(
            "2021-02-05",
            "If Moritte copies a nonlegendary permanent, you'll control two permanents with same name, but only " +
                "one of them (Moritte) will be legendary. You won't put any of them into their owner's graveyard."
        )
        ruling(
            "2021-02-05",
            "If Moritte copies a creature, it enters with two +1/+1 counters on it, plus any counters that will " +
                "be put on it by abilities it copied or abilities of other objects."
        )
        ruling(
            "2021-02-05",
            "If Moritte copies a permanent that's normally not a creature but has been made a creature by " +
                "another effect, Moritte enters as a noncreature permanent that doesn't get +1/+1 counters or have changeling."
        )
    }
}
