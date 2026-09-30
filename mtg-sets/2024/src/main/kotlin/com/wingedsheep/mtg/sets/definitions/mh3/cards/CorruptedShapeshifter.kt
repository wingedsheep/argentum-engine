package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntryCharacteristics
import com.wingedsheep.sdk.scripting.ModeOption

/**
 * Corrupted Shapeshifter
 * {3}{U}
 * Creature — Eldrazi Shapeshifter
 * star/star
 *
 * Devoid
 * As this creature enters, it becomes your choice of a 3/3 creature with flying, a 2/5 creature
 * with vigilance, or a 0/12 creature with defender.
 *
 * Each option `becomes` its shape: the chosen P/T and keyword are written into the permanent's
 * copiable values (CR 707.2), so a copy of it is the chosen shape without choosing again.
 */
val CorruptedShapeshifter = card("Corrupted Shapeshifter") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Shapeshifter"
    // Printed star/star: a 0/0 creature card anywhere but the battlefield (first ruling).
    power = 0
    toughness = 0
    oracleText = "Devoid (This card has no color.)\nAs this creature enters, it becomes your choice of " +
        "a 3/3 creature with flying, a 2/5 creature with vigilance, or a 0/12 creature with defender."

    keywords(Keyword.DEVOID)

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "3/3 flying", label = "3/3 creature with flying",
                    becomes = EntryCharacteristics(3, 3, keywords = setOf(Keyword.FLYING)),
                ),
                ModeOption(
                    id = "2/5 vigilance", label = "2/5 creature with vigilance",
                    becomes = EntryCharacteristics(2, 5, keywords = setOf(Keyword.VIGILANCE)),
                ),
                ModeOption(
                    id = "0/12 defender", label = "0/12 creature with defender",
                    becomes = EntryCharacteristics(0, 12, keywords = setOf(Keyword.DEFENDER)),
                ),
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Ralph Horsley"
        flavorText = "Within its body, three lineages warred for supremacy."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27534c8a-cb71-43bc-b706-80a525396222.jpg?1783911293"
        ruling("2024-06-07", "While not on the battlefield, Corrupted Shapeshifter is a 0/0 creature card. It doesn't have flying, vigilance, or defender.")
        ruling("2024-06-07", "If an object on the battlefield becomes a copy of Corrupted Shapeshifter, it copies the values determined by its enters-the-battlefield replacement effect.")
        ruling("2024-06-07", "If a permanent enters the battlefield as a copy of Corrupted Shapeshifter, there will be two copy effects to apply to it. One will copy the values chosen for the Corrupted Shapeshifter being copied. And you'll choose the values for the copy's own enters-the-battlefield replacement effect. The resulting permanent will have the abilities from both effects, and its power and toughness will be determined by the last effect to apply. For example, if a creature entering the battlefield is copying an 0/12 Corrupted Shapeshifter with defender, you could make it a 3/3 or an 0/12, either way with flying and defender.")
    }
}
