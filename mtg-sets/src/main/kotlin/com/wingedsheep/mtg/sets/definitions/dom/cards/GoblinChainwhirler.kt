package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Goblin Chainwhirler
 * {R}{R}{R}
 * Creature — Goblin Warrior
 * 3/3
 * First strike
 * When Goblin Chainwhirler enters the battlefield, it deals 1 damage to each opponent
 * and each creature and planeswalker they control.
 */
val GoblinChainwhirler = card("Goblin Chainwhirler") {
    manaCost = "{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 3
    toughness = 3
    oracleText = "First strike\nWhen this creature enters, it deals 1 damage to each opponent and each creature and planeswalker they control."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
            .then(GroupPatterns.dealDamageToAll(1, GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls())))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Svetlin Velinov"
        flavorText = "\"The trick is, once you get moving, don't stop!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bdb2883-3cfd-4fb0-9f99-6a57277f7fe4.jpg?1562739277"
    }
}
