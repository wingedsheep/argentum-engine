package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Gandalf, Friend of the Shire
 * {3}{U}
 * Legendary Creature — Avatar Wizard
 * 2/4
 *
 * Flash
 * You may cast sorcery spells as though they had flash.
 * Whenever the Ring tempts you, if you chose a creature other than Gandalf as your
 * Ring-bearer, draw a card.
 *
 * The "sorcery-speed → flash" permission is the permanent-static
 * [GrantFlashToSpellType] (CR 702.8a) filtered to sorcery spells and scoped to the
 * controller. The Ring-tempts trigger composes via the existing
 * [Conditions.YouChoseOtherCreatureAsRingBearer] (Gap 3 in LTR's TODO).
 */
val GandalfFriendOfTheShire = card("Gandalf, Friend of the Shire") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 2
    toughness = 4
    oracleText = "Flash\n" +
        "You may cast sorcery spells as though they had flash.\n" +
        "Whenever the Ring tempts you, if you chose a creature other than Gandalf as your " +
        "Ring-bearer, draw a card."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Sorcery,
            controllerOnly = true
        )
    }

    triggeredAbility {
        trigger = Triggers.RingTemptsYou
        triggerCondition = Conditions.YouChoseOtherCreatureAsRingBearer
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Dmitry Burmak"
        flavorText = "\"After a hundred years, Hobbits can still surprise you at a pinch.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc9cfcc7-be64-4871-8d52-851e43fe3305.jpg?1696231214"
    }
}
