package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Graveblade Marauder
 * {2}{B}
 * Creature — Human Warrior
 * 1/4
 *
 * Deathtouch
 * Whenever this creature deals combat damage to a player, that player loses life equal to the
 * number of creature cards in your graveyard.
 *
 * The count is taken at resolution, not when the damage is dealt.
 */
val GravebladeMarauder = card("Graveblade Marauder") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 4
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\n" +
        "Whenever this creature deals combat damage to a player, that player loses life equal to " +
        "the number of creature cards in your graveyard."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.LoseLife(
            amount = DynamicAmounts.creatureCardsInYourGraveyard(),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
        description = "Whenever this creature deals combat damage to a player, that player loses " +
            "life equal to the number of creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85880be1-65b2-432f-8788-79716e4c8066.jpg?1783938340"
    }
}
