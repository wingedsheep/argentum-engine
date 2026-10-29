package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Warden of the Woods (Core Set 2021 #213)
 * {4}{G}{G} · Creature — Treefolk · 5/7
 *
 * Vigilance
 * Whenever this creature becomes the target of a spell or ability an opponent controls, you may
 * draw two cards.
 */
val WardenOfTheWoods = card("Warden of the Woods") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 5
    toughness = 7
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "Whenever this creature becomes the target of a spell or ability an opponent controls, you may draw two cards."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(byOpponent = true)
        optional = true
        effect = Effects.DrawCards(2)
        description = "Whenever this creature becomes the target of a spell or ability an opponent " +
            "controls, you may draw two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Svetlin Velinov"
        flavorText = "Weak and strong alike seek shelter beneath its branches."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1d81cfc-cff8-4478-92b6-efbfc5084165.jpg?1783930665"
        ruling("2020-06-23", "An ability that triggers when something becomes the target of a spell or ability resolves before the spell or ability that caused it to trigger. It resolves even if that spell or ability is countered.")
        ruling("2020-06-23", "Players can cast spells and activate abilities after the triggered ability resolves but before the spell that caused it to trigger does.")
    }
}
