package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntryCharacteristics
import com.wingedsheep.sdk.scripting.ModeOption

/**
 * Primal Clay
 * {4}
 * Artifact Creature — Shapeshifter
 * Power/toughness: star/star
 *
 * As this creature enters, it becomes your choice of a 3/3 artifact creature, a 2/2 artifact
 * creature with flying, or a 1/6 Wall artifact creature with defender in addition to its other
 * types.
 *
 * Implementation: each [EntersWithChoice] MODE option `becomes` its shape — the chosen P/T,
 * keyword and Wall subtype are written into the permanent's copiable values (CR 707.2), so a copy
 * of Primal Clay is whatever it became, without choosing.
 */
val PrimalClay = card("Primal Clay") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Shapeshifter"
    // Printed power/toughness are star/star: a 0/0 anywhere but the battlefield. The chosen
    // option's power/toughness replace these as it enters.
    power = 0
    toughness = 0
    oracleText = "As this creature enters, it becomes your choice of a 3/3 artifact creature, a 2/2 " +
        "artifact creature with flying, or a 1/6 Wall artifact creature with defender in addition to " +
        "its other types. (A creature with defender can't attack.)"

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = "3/3", label = "3/3 artifact creature", becomes = EntryCharacteristics(3, 3)),
                ModeOption(
                    id = "2/2 flying", label = "2/2 artifact creature with flying",
                    becomes = EntryCharacteristics(2, 2, keywords = setOf(Keyword.FLYING)),
                ),
                ModeOption(
                    id = "1/6 defender", label = "1/6 Wall artifact creature with defender",
                    becomes = EntryCharacteristics(1, 6, keywords = setOf(Keyword.DEFENDER), subtypes = listOf("Wall")),
                ),
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab9d0e3f-cf7c-41f8-bcd7-bb08ea8cc2f8.jpg?1562931210"
    }
}
