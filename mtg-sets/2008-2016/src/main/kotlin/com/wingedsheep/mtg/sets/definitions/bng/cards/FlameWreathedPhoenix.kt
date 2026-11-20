package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.tribute
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flame-Wreathed Phoenix
 * {2}{R}{R}
 * Creature — Phoenix
 * 3/3
 *
 * Flying
 * Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)
 * When this creature enters, if tribute wasn't paid, it gains haste and "When this creature dies,
 * return it to its owner's hand."
 *
 * Both gains have no stated end, so they last indefinitely (`Duration.Permanent`). The granted dies
 * trigger returns the card only from the graveyard (`fromZone`), so a card that has moved on again
 * before the trigger resolves is a new object (CR 400.7) and stays where it is.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val FlameWreathedPhoenix = card("Flame-Wreathed Phoenix") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Tribute 2 (As this creature enters, an opponent of your choice may put two +1/+1 counters on it.)\n" +
        "When this creature enters, if tribute wasn't paid, it gains haste and " +
        "\"When this creature dies, return it to its owner's hand.\""

    keywords(Keyword.FLYING)
    tribute(2)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.TributeNotPaid
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self, Duration.Permanent) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD),
                    descriptionOverride = "When this creature dies, return it to its owner's hand.",
                ),
                target = EffectTarget.Self,
                duration = Duration.Permanent,
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "97"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a36bca58-e92f-4496-92d6-5ecba7a0424d.jpg?1783939545"
        ruling("2017-04-18", "For effects that check which player put counters on the entering creature, the player chosen to pay tribute puts those counters on it, not the creature's controller.")
        ruling("2014-02-01", "If the opponent pays tribute, the creature will enter the battlefield with the specified number of +1/+1 counters on it. It won't enter the battlefield and then have the +1/+1 counters placed on it.")
        ruling("2014-02-01", "Players can't respond to the tribute decision before the creature enters the battlefield. That is, if the opponent doesn't pay tribute, the triggered ability will trigger before any player has a chance to remove the creature.")
        ruling("2014-02-01", "The triggered ability will resolve even if the creature with tribute isn't on the battlefield at that time.")
    }
}
