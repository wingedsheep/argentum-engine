package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fae of Wishes // Granted
 * {1}{U}
 * Creature — Faerie Wizard
 * 1/4
 *
 * Flying
 * {1}{U}, Discard two cards: Return this creature to its owner's hand.
 *
 * Adventure: Granted — {3}{U}, Sorcery — Adventure
 * You may reveal a noncreature card you own from outside the game and put it into your hand.
 *
 * "Outside the game" is the owner's sideboard ([Patterns.Sideboard.wish], as Burning Wish):
 * gather the noncreature cards there, choose up to one (the "may"), reveal it, put it into hand.
 */
val FaeOfWishes = card("Fae of Wishes") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Wizard"
    power = 1
    toughness = 4
    oracleText = "Flying\n{1}{U}, Discard two cards: Return this creature to its owner's hand."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Discard(count = 2))
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "{1}{U}, Discard two cards: Return this creature to its owner's hand."
    }

    adventure("Granted") {
        manaCost = "{3}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "You may reveal a noncreature card you own from outside the game and put it " +
            "into your hand. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Patterns.Sideboard.wish(GameObjectFilter.Noncreature)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3435fd6-8f51-4d99-a278-4ddb088acfe1.jpg?1783932659"
        ruling(
            "2019-10-04",
            "The activated ability of Fae of Wishes can be activated only while Fae of Wishes is on the battlefield."
        )
        ruling(
            "2019-10-04",
            "In a casual game, a card you choose from outside the game comes from your personal collection. " +
                "In a tournament event, a card you choose from outside the game must come from your sideboard. " +
                "You may look at your sideboard any time."
        )
    }
}
