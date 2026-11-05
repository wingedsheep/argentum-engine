package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Nimble Obstructionist
 * {2}{U}
 * Creature — Bird Wizard
 * 3/1
 * Flash
 * Flying
 * Cycling {2}{U}
 * When you cycle this card, counter target activated or triggered ability you don't control.
 *
 * "You don't control" is `Not(ControlledByYou)` (as Vandalblast), not "an opponent controls"; the
 * stack controller lookup reads the ability's own controller. The cycling ability itself is yours,
 * so it is never a legal target — with nothing else to counter, the trigger is simply removed and
 * the cycling draw still happens (ruling 2022-12-08).
 */
val NimbleObstructionist = card("Nimble Obstructionist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    oracleText = "Flash\nFlying\nCycling {2}{U} ({2}{U}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, counter target activated or triggered ability you don't control."
    power = 3
    toughness = 1

    keywords(Keyword.FLASH, Keyword.FLYING)
    keywordAbility(KeywordAbility.cycling("{2}{U}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        target(
            TargetFilter.ActivatedOrTriggeredAbilityOnStack.withControllerPredicate(
                ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
            )
        )
        effect = Effects.CounterAbility()
        description = "When you cycle this card, counter target activated or triggered ability you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Shreya Shetty"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26012b65-7e0a-427e-945e-de24738517fb.jpg?1783936052"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
        ruling("2017-07-14", "If you counter a delayed triggered ability that triggers at the beginning of the \"next\" occurrence of a specified step or phase, that ability won't trigger again the following time that phase or step occurs.")
        ruling("2017-07-14", "Abilities that create replacement effects, such as a permanent entering the battlefield tapped or with counters on it, can't be targeted. Abilities that apply \"as [this creature] enters the battlefield\" are also replacement effects and can't be targeted.")
        ruling("2017-07-14", "Mana abilities can't be targeted. An activated mana ability is one that could add mana as it resolves, doesn't have a target, and isn't a loyalty ability. A triggered mana ability is one that could add mana as it resolves and triggers on an activated mana ability.")
    }
}
