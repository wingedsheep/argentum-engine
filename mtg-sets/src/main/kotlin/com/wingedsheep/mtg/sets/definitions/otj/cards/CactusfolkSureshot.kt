package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Cactusfolk Sureshot — Outlaws of Thunder Junction #199
 * {2}{R}{G} · Creature — Plant Mercenary · Uncommon
 * 4/4
 *
 * Reach
 * Ward {2}
 * At the beginning of combat on your turn, other creatures you control with power 4 or
 * greater gain trample and haste until end of turn.
 *
 * The combat trigger grants both keywords to every *other* creature you control with power
 * ≥ 4 at resolution via [Patterns.Group] (iterates the projected battlefield, so a creature
 * whose power drops below 4 before resolution is skipped, and Cactusfolk itself is excluded).
 */
val CactusfolkSureshot = card("Cactusfolk Sureshot") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Plant Mercenary"
    power = 4
    toughness = 4
    oracleText = "Reach\n" +
        "Ward {2} (Whenever this creature becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {2}.)\n" +
        "At the beginning of combat on your turn, other creatures you control with power 4 " +
        "or greater gain trample and haste until end of turn."

    keywords(Keyword.REACH)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.TRAMPLE,
            Filters.Group.creaturesYouControl.powerAtLeast(4).other()
        ).then(
            Patterns.Group.grantKeywordToAll(
                Keyword.HASTE,
                Filters.Group.creaturesYouControl.powerAtLeast(4).other()
            )
        )
        description = "other creatures you control with power 4 or greater gain trample and " +
            "haste until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/318b8c5d-9fb0-488f-9b32-c2e29d1f1dbb.jpg?1712356071"
    }
}
