package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kitesail Cleric — Zendikar Rising #20
 * {W} · Creature — Kor Cleric · 1/1
 *
 * Kicker {2}{W}
 * Flying
 * When this creature enters, if it was kicked, tap up to two target creatures.
 *
 * The kicked check is an intervening "if": an unkicked Cleric never puts the trigger on the stack.
 */
val KitesailCleric = card("Kitesail Cleric") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Cleric"
    oracleText = "Kicker {2}{W} (You may pay an additional {2}{W} as you cast this spell.)\n" +
        "Flying\n" +
        "When this creature enters, if it was kicked, tap up to two target creatures."
    power = 1
    toughness = 1

    keywordAbility(KeywordAbility.kicker("{2}{W}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.TapEachTarget()
        description = "When this creature enters, if it was kicked, tap up to two target creatures."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "PINDURSKI"
        flavorText = "Too flashy to ignore. Too fast to catch."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16626f84-00d3-4ec9-b62c-19fff380cd3a.jpg"
    }
}
