package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Heartstabber Mosquito
 * {3}{B}
 * Creature — Insect
 * 2/2
 *
 * Kicker {2}{B}
 * Flying
 * When this creature enters, if it was kicked, destroy target creature.
 *
 * The kicked check is an intervening "if" ([WasKicked]): unkicked, the trigger never goes on the
 * stack, so no target is chosen.
 */
val HeartstabberMosquito = card("Heartstabber Mosquito") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    power = 2
    toughness = 2
    oracleText = "Kicker {2}{B} (You may pay an additional {2}{B} as you cast this spell.)\n" +
        "Flying\n" +
        "When this creature enters, if it was kicked, destroy target creature."

    keywordAbility(KeywordAbility.kicker("{2}{B}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        val t = target(TargetFilter.Creature)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d20016d-2b5a-445e-a59b-5305e76f549c.jpg?1783942153"
    }
}
