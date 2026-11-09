package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Venser, Shaper Savant
 * {2}{U}{U}
 * Legendary Creature — Human Wizard
 * 2/2
 *
 * Flash
 * When Venser enters, return target spell or permanent to its owner's hand.
 *
 * Bouncing a spell removes it from the stack without countering it, so it works on a spell that
 * can't be countered; a spell cast with flashback is exiled instead (the executor's stack-exit rider).
 */
val VenserShaperSavant = card("Venser, Shaper Savant") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When Venser enters, return target spell or permanent to its owner's hand."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetSpellOrPermanent())
        effect = Effects.ReturnSpellOrPermanentToOwnersHand(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Aleksi Briclot"
        flavorText = "His marvels of artifice pale in comparison to the developing machinery of his mind."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e84fc99-4045-4518-b588-512a675f2933.jpg?1783943119"
        ruling(
            "2021-03-19",
            "If Venser's triggered ability targets a spell cast with flashback, that spell will be " +
                "exiled instead of returning to its owner's hand."
        )
        ruling(
            "2021-03-19",
            "If a spell is returned to its owner's hand, it's removed from the stack and thus will not " +
                "resolve. The spell isn't countered; it just no longer exists. This works even against a " +
                "spell that can't be countered."
        )
        ruling(
            "2021-03-19",
            "If a copy of a spell is returned to its owner's hand, it's moved there, then it will cease " +
                "to exist as a state-based action. It can't be recast."
        )
    }
}
