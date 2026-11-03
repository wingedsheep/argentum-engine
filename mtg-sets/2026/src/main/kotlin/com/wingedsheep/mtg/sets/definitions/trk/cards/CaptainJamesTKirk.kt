package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Captain James T. Kirk
 * {2}{R}
 * Legendary Creature — Human Officer
 * 2/2
 * Whenever Captain Kirk enters or attacks, choose one. If you have no cards in hand, choose one or
 * more instead.
 * • Discard a card, then draw a card.
 * • Create a 1/1 red Officer creature token.
 * • Creatures you control get +1/+0 until end of turn.
 *
 * Modeling notes:
 *  - "Enters or attacks" is two triggers (enters, attacks) sharing one modal, as Borg Cube does.
 *  - The mode count is fixed as the trigger is put on the stack (CR 603.3c): `dynamicChooseCount`
 *    yields 3 when your hand is empty, else 1, and `dynamicMinChooseCount` holds the floor at one —
 *    the Disciple of Perdition shape, with "one or more" rather than "both".
 */
private val kirkModal = Effects.Modal(
    modes = listOf(
        Mode.noTarget(
            effect = Patterns.Hand.discardCards(1) then Effects.DrawCards(1),
            description = "Discard a card, then draw a card."
        ),
        Mode.noTarget(
            effect = Effects.CreateToken(
                power = 1, toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Officer"),
                imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
            ),
            description = "Create a 1/1 red Officer creature token."
        ),
        Mode.noTarget(
            effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl())),
            description = "Creatures you control get +1/+0 until end of turn."
        )
    ),
    dynamicChooseCount = DynamicAmounts.conditional(Conditions.EmptyHand, 3, 1),
    dynamicMinChooseCount = DynamicAmounts.fixed(1),
)

private const val KIRK_MODES = "choose one. If you have no cards in hand, choose one or more instead. " +
    "Discard a card, then draw a card.; Create a 1/1 red Officer creature token.; " +
    "Creatures you control get +1/+0 until end of turn."

val CaptainJamesTKirk = card("Captain James T. Kirk") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Officer"
    power = 2
    toughness = 2
    oracleText = "Whenever Captain Kirk enters or attacks, choose one. If you have no cards in hand, " +
        "choose one or more instead.\n" +
        "• Discard a card, then draw a card.\n" +
        "• Create a 1/1 red Officer creature token.\n" +
        "• Creatures you control get +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = kirkModal
        description = "Whenever Captain Kirk enters, $KIRK_MODES"
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = kirkModal
        description = "Whenever Captain Kirk attacks, $KIRK_MODES"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "142"
        artist = "Jason Pastrana"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40a0466d-20bb-4ed5-858d-947fb9144580.jpg?1784036779"
    }
}
