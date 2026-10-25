package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ashaya, Soul of the Wild
 * {3}{G}{G}
 * Legendary Creature — Elemental
 * star/star
 * Ashaya's power and toughness are each equal to the number of lands you control.
 * Nontoken creatures you control are Forest lands in addition to their other types.
 * (They're still affected by summoning sickness.)
 *
 * Two Layer 4 grants over the same group: the LAND card type and the Forest subtype. The engine
 * derives the intrinsic "{T}: Add {G}." from the *projected* type line, so every nontoken creature
 * you control — Ashaya included, which therefore counts itself — taps for green, subject to
 * summoning sickness because it is still a creature.
 */
private val nontokenCreaturesYouControl = GroupFilter(GameObjectFilter.Creature.nontoken().youControl())

val AshayaSoulOfTheWild = card("Ashaya, Soul of the Wild") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elemental"
    oracleText = "Ashaya's power and toughness are each equal to the number of lands you control.\n" +
        "Nontoken creatures you control are Forest lands in addition to their other types. " +
        "(They're still affected by summoning sickness.)"

    dynamicStats(DynamicAmounts.landsYouControl())

    staticAbility {
        ability = GrantCardType(cardType = "LAND", filter = nontokenCreaturesYouControl)
    }
    staticAbility {
        ability = GrantSubtype("Forest", nontokenCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "179"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74943390-d25f-47cb-90bb-cbf70c87f4a2.jpg?1783929342"
        ruling(
            "2020-09-25",
            "The ability that defines Ashaya's power and toughness applies in all zones, not just the battlefield."
        )
        ruling(
            "2020-09-25",
            "As long as Ashaya is on the battlefield, it's affected by its second ability and thus its first " +
                "ability counts itself."
        )
        ruling(
            "2020-09-25",
            "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal " +
                "damage dealt to Ashaya may become lethal if lands you control leave the battlefield during that turn."
        )
        ruling(
            "2020-09-25",
            "You can't play creature cards as lands; you'll still have to cast them as spells, and they'll enter " +
                "the battlefield as lands (in addition to their other types)."
        )
        ruling("2020-09-25", "A land that's a Forest has the intrinsic mana ability \"{T}: Add {G}.\"")
    }
}
