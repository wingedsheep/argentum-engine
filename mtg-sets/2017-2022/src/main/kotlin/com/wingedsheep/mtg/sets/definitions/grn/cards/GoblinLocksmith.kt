package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin Locksmith
 * {1}{R}
 * Creature — Goblin Rogue
 * 2/1
 *
 * Whenever this creature attacks, creatures with defender can't block this turn.
 *
 * The block restriction is a dynamic group restriction (it doesn't change characteristics), so it
 * applies to every creature with defender this turn, including ones that gain defender later.
 */
val GoblinLocksmith = card("Goblin Locksmith") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Rogue"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks, creatures with defender can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CantBlockGroup(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Mark Behm"
        flavorText = "\"He's got more finesse than most goblins. He smashes windows with his fist rather " +
            "than his forehead.\"\n—Arrester Polgar"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf2bed0a-990a-499a-af0a-52b77557d7cb.jpg"
    }
}
