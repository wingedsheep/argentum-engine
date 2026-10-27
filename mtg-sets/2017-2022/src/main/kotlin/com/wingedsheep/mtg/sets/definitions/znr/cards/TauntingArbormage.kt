package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Taunting Arbormage — Zendikar Rising #212
 * {2}{G} · Creature — Elf Wizard · 2/3
 *
 * Kicker {3}
 * When this creature enters, if it was kicked, all creatures able to block target creature this
 * turn do so.
 *
 * The kicked check is an intervening "if"; the lure is the every-blocker form of
 * [Effects.MustBeBlocked] (as on Taunting Challenge).
 */
val TauntingArbormage = card("Taunting Arbormage") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Wizard"
    oracleText = "Kicker {3} (You may pay an additional {3} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, all creatures able to block target creature this turn do so."
    power = 2
    toughness = 3

    keywordAbility(KeywordAbility.kicker("{3}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val t = target(TargetFilter.Creature)
        effect = Effects.MustBeBlocked(t, allCreatures = true)
        description = "When this creature enters, if it was kicked, all creatures able to block target creature this turn do so."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Cristi Balanescu"
        flavorText = "\"Eyes on me!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca6f8996-bf72-4e55-944f-5b8202b6068c.jpg"
    }
}
