package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Runic Armasaur
 * {1}{G}{G}
 * Creature — Dinosaur
 * 2/5
 * Whenever an opponent activates an ability of a creature or land that isn't a mana ability, you
 * may draw a card.
 *
 * "A creature or land" names permanents: the rulings rule out cycling from hand and graveyard
 * abilities, hence `.onBattlefield()`, and a crewed Vehicle isn't a creature yet when crew is
 * activated. A creature or land its own cost sacrifices is read as it last existed (CR 113.7a).
 */
val RunicArmasaur = card("Runic Armasaur") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 5
    oracleText = "Whenever an opponent activates an ability of a creature or land that isn't a mana ability, you may draw a card."

    triggeredAbility {
        trigger = Triggers.anOpponent.activatesAbility(
            of = (GameObjectFilter.Creature or GameObjectFilter.Land).onBattlefield()
        )
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever an opponent activates an ability of a creature or land that isn't a mana ability, you may draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Randy Vargas"
        flavorText = "The Sun Empire's most sacred sites are protected by more than prayers."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b445a0f0-2cca-4223-bd27-940d2cb6d29c.jpg?1783934527"
        ruling(
            "2018-07-13",
            "Runic Armasaur's ability doesn't trigger when an opponent activates an ability of a card in hand " +
                "(such as a cycling ability from the Amonkhet block) or a card in a graveyard (such as that of Bone Dragon), " +
                "even if that causes a card to be put onto the battlefield."
        )
        ruling(
            "2018-07-13",
            "Runic Armasaur's ability doesn't trigger when an opponent activates an ability of a noncreature, " +
                "nonland permanent that causes it to become a creature (such as a crew ability of a Vehicle)."
        )
    }
}
