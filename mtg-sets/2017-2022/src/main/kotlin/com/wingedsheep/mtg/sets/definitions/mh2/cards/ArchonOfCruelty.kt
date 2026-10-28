package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Archon of Cruelty — Modern Horizons 2 #75
 * {6}{B}{B} · Creature — Archon · 6/6
 *
 * Flying
 * Whenever this creature enters or attacks, target opponent sacrifices a creature or
 * planeswalker of their choice, discards a card, and loses 3 life. You draw a card and gain
 * 3 life.
 *
 * "Enters or attacks" is two triggers sharing one effect (Grave Titan's shape). The actions run
 * in printed order, so the sacrificed permanent is gone before the discard and life loss.
 */
val ArchonOfCruelty = card("Archon of Cruelty") {
    manaCost = "{6}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Archon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Whenever this creature enters or attacks, target opponent sacrifices a creature or planeswalker " +
        "of their choice, discards a card, and loses 3 life. You draw a card and gain 3 life."

    keywords(Keyword.FLYING)

    val triggerDescription = "Whenever this creature enters or attacks, target opponent sacrifices a " +
        "creature or planeswalker of their choice, discards a card, and loses 3 life. You draw a card " +
        "and gain 3 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.CreatureOrPlaneswalker, target = opponent) then
            Effects.Discard(1, opponent) then
            Effects.LoseLife(3, opponent) then
            Effects.DrawCards(1) then
            Effects.GainLife(3)
        description = triggerDescription
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.CreatureOrPlaneswalker, target = opponent) then
            Effects.Discard(1, opponent) then
            Effects.LoseLife(3, opponent) then
            Effects.DrawCards(1) then
            Effects.GainLife(3)
        description = triggerDescription
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "75"
        artist = "Andrew Mar"
        flavorText = "Malice spreads on wings of tyranny."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1be9d9a4-d7ee-4854-abc2-85cabf993ec9.jpg?1783926865"
        ruling(
            "2021-06-18",
            "You and the opponent you target take all the actions in the order listed. Notably, the creature or planeswalker they sacrifice won't be on the battlefield as they discard a card or lose life. If any abilities trigger, they will wait to be put on the stack until after the spell resolves. If any abilities controlled by that opponent trigger but losing 3 life causes them to lose the game, none of those abilities will be put on the stack."
        )
    }
}
