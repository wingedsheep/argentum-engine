package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Oath of Teferi
 * {3}{W}{U}
 * Legendary Enchantment
 * When Oath of Teferi enters, exile another target permanent you control. Return it to the
 * battlefield under its owner's control at the beginning of the next end step.
 * You may activate the loyalty abilities of planeswalkers you control twice each turn rather
 * than only once.
 */
val OathOfTeferi = card("Oath of Teferi") {
    manaCost = "{3}{W}{U}"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Teferi enters, exile another target permanent you control. Return it to the battlefield under its owner's control at the beginning of the next end step.\nYou may activate the loyalty abilities of planeswalkers you control twice each turn rather than only once."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("permanent", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Permanent.youControl(), excludeSelf = true)
        ))
        effect = EffectPatterns.exileUntilEndStep(t)
    }

    staticAbility {
        ability = ExtraLoyaltyActivation
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Wesley Burt"
        flavorText = "\"For the lost and forgotten, I will keep watch.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a7fadca-0a94-4624-ab95-2c2410829824.jpg?1562736186"
        ruling("2018-04-27", "If a token is exiled this way, it ceases to exist and won't return to the battlefield.")
        ruling("2018-04-27", "For Oath of Teferi's last ability, you may activate the same ability of a planeswalker twice, or you may activate two different abilities of that planeswalker.")
        ruling("2018-04-27", "If you control more than one Oath of Teferi, you won't be able to activate abilities of any one planeswalker more than twice in one turn.")
    }
}
