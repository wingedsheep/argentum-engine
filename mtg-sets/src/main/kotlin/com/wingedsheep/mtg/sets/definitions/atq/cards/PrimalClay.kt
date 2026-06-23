package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

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
 * Implementation: the entry choice is recorded via the generic [EntersWithChoice] (ChoiceType.MODE),
 * which locks a stable mode id onto the permanent for its life (CR 614.1c / 604.3 — the chosen P/T
 * and keyword are then fixed characteristic-defining abilities). Each mode's continuous effects are
 * gated by [SourceChosenModeIs] so only the chosen one applies: a [SetBasePowerToughnessStatic] CDA
 * sets the base P/T (the printed P/T is star/star) and a [GrantKeyword] / [GrantSubtype] supplies the
 * mode's keyword/type. The "1/6 Wall with defender" mode adds both the Wall subtype and defender.
 */
val PrimalClay = card("Primal Clay") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Shapeshifter"
    // Printed power/toughness are star/star; placeholder 0/0 base is overwritten in Layer 7b by the
    // chosen mode's SetBasePowerToughnessStatic CDA. A mode is always chosen as it enters, so the
    // creature never actually sits at 0/0.
    power = 0
    toughness = 0
    oracleText = "As this creature enters, it becomes your choice of a 3/3 artifact creature, a 2/2 " +
        "artifact creature with flying, or a 1/6 Wall artifact creature with defender in addition to " +
        "its other types. (A creature with defender can't attack.)"

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(id = "3/3", label = "3/3 artifact creature"),
                ModeOption(id = "2/2 flying", label = "2/2 artifact creature with flying"),
                ModeOption(id = "1/6 defender", label = "1/6 Wall artifact creature with defender"),
            )
        )
    )

    // 3/3 mode.
    staticAbility {
        condition = SourceChosenModeIs("3/3")
        ability = SetBasePowerToughnessStatic(power = 3, toughness = 3, filter = GroupFilter.source())
    }

    // 2/2 with flying mode.
    staticAbility {
        condition = SourceChosenModeIs("2/2 flying")
        ability = SetBasePowerToughnessStatic(power = 2, toughness = 2, filter = GroupFilter.source())
    }
    staticAbility {
        condition = SourceChosenModeIs("2/2 flying")
        ability = GrantKeyword(keyword = Keyword.FLYING, filter = GroupFilter.source())
    }

    // 1/6 Wall with defender mode.
    staticAbility {
        condition = SourceChosenModeIs("1/6 defender")
        ability = SetBasePowerToughnessStatic(power = 1, toughness = 6, filter = GroupFilter.source())
    }
    staticAbility {
        condition = SourceChosenModeIs("1/6 defender")
        ability = GrantSubtype(subtype = "Wall", filter = GroupFilter.source())
    }
    staticAbility {
        condition = SourceChosenModeIs("1/6 defender")
        ability = GrantKeyword(keyword = Keyword.DEFENDER, filter = GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab9d0e3f-cf7c-41f8-bcd7-bb08ea8cc2f8.jpg?1562931210"
    }
}
