package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Underrealm Lich
 * {3}{B}{G}
 * Creature — Zombie Elf Shaman
 * 4/3
 * If you would draw a card, instead look at the top three cards of your library, then put one into
 * your hand and the rest into your graveyard.
 * Pay 4 life: This creature gains indestructible until end of turn. Tap it.
 */
val UnderrealmLich = card("Underrealm Lich") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Zombie Elf Shaman"
    power = 4
    toughness = 3
    oracleText = "If you would draw a card, instead look at the top three cards of your library, then put " +
        "one into your hand and the rest into your graveyard.\n" +
        "Pay 4 life: This creature gains indestructible until end of turn. Tap it."

    // Each draw is replaced one at a time (2018-10-05 ruling); the replacement applies with fewer
    // than three cards in the library, and with none it replaces the draw so you don't lose for it.
    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Patterns.Library.lookAtTopAndKeep(count = 3, keepCount = 1)
        )
    )

    activatedAbility {
        cost = Costs.PayLife(4)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self) then
            Effects.Tap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Anna Steinbauer"
        flavorText = "\"You've strayed, overworlder.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0782e090-209c-428f-966a-17f3ceab2903.jpg?1783934118"
        ruling(
            "2018-10-05",
            "If an effect instructs you to draw multiple cards, each draw is performed—and replaced by " +
                "Underrealm Lich's ability—one at a time. This means that if you're drawing two cards, you'll " +
                "look at the top three cards of your library, put one into your hand, put the other two into " +
                "your graveyard, and then repeat the process with the next three cards in your library."
        )
        ruling(
            "2018-10-05",
            "Underrealm Lich's first ability applies even if you have fewer than three cards in your " +
                "library. If you have no cards in your library, you can't lose the game for trying to draw " +
                "a card from an empty library."
        )
        ruling(
            "2018-10-05",
            "You can activate Underrealm Lich's last ability even if it's already tapped. It can't be " +
                "tapped again, but it still gains indestructible."
        )
    }
}
