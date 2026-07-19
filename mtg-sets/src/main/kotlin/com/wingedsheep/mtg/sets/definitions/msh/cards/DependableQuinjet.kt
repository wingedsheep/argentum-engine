package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Dependable Quinjet
 * {3}
 * Artifact — Vehicle
 * 3/3
 *
 * Flying
 * {T}: Add one mana of any color.
 * Crew 4
 *
 * A Vehicle is a noncreature artifact until crewed, so the {T} mana ability is usable the turn it
 * enters. [Effects.AddManaOfChoice] with the default `ManaColorSet.AnyColor` is the canonical
 * "add one mana of any color"; crew is [KeywordAbility.crew].
 */
val DependableQuinjet = card("Dependable Quinjet") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "{T}: Add one mana of any color.\n" +
        "Crew 4 (Tap any number of creatures you control with total power 4 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.crew(4))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "246"
        artist = "Oliver Wetter"
        flavorText = "\"Eh, I've flown enough Quinjets to know not to get too attached.\"\n—She-Hulk, Jennifer Walters"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c035c625-4de5-4c3b-9d07-aa1df8fc7b78.jpg?1783902893"
    }
}
