package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Warden of the Wall
 * {3}
 * Artifact
 * This artifact enters tapped.
 * {T}: Add {C}.
 * During turns other than yours, this artifact is a 2/3 Gargoyle artifact creature with flying.
 *
 * The animation is the Gideon Blackblade shape inverted: not-your-turn conditional statics on the
 * source that add the creature type and the Gargoyle subtype, set base P/T to 2/3, and grant
 * flying. It stays an artifact throughout, and keeps its mana ability.
 */
val WardenOfTheWall = card("Warden of the Wall") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}: Add {C}.\n" +
        "During turns other than yours, this artifact is a 2/3 Gargoyle artifact creature with flying."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // "During turns other than yours, this artifact is a 2/3 Gargoyle artifact creature with flying."
    staticAbility {
        condition = Conditions.IsNotYourTurn
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.IsNotYourTurn
        ability = GrantSubtype("Gargoyle", GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.IsNotYourTurn
        ability = SetBasePowerToughnessStatic(2, 3, GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.IsNotYourTurn
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd1a1f48-d46b-4b8e-a642-fc70fd9ef7df.jpg?1783940791"
        ruling("2011-01-22", "Warden of the Wall still has the ability to tap for mana when it's not your turn.")
    }
}
