package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantProtectionToController
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Runed Halo
 * {W}{W}
 * Enchantment
 * As this enchantment enters, choose a card name.
 * You have protection from the chosen card name. (You can't be targeted, dealt damage, or enchanted
 * by anything with that name.)
 */
val RunedHalo = card("Runed Halo") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a card name.\n" +
        "You have protection from the chosen card name. " +
        "(You can't be targeted, dealt damage, or enchanted by anything with that name.)"

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
        )
    )

    staticAbility {
        ability = GrantProtectionToController(ProtectionScope.ChosenCardName())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92b45f79-f23e-4eef-8cac-91780dc2a044.jpg?1783942765"
    }
}
