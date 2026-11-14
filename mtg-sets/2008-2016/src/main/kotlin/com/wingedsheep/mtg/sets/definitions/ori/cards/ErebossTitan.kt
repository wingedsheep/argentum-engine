package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Erebos's Titan
 * {1}{B}{B}{B}
 * Creature — Giant
 * 5/5
 *
 * As long as your opponents control no creatures, this creature has indestructible.
 * Whenever a creature card leaves an opponent's graveyard, you may discard a card. If you do,
 * return this card from your graveyard to your hand.
 *
 * Modeling notes:
 *  - Indestructible is a continuous grant gated on "no opponent controls a creature"
 *    (`OpponentControls(Creature, negate = true)`), re-evaluated on every projection — so the
 *    keyword switches off the moment an opponent gets a creature, and lethal damage already
 *    marked on the Titan then destroys it (2015-06-22 ruling).
 *  - The recursion trigger functions only from the graveyard (`triggerZone = GRAVEYARD`). "Leaves
 *    an opponent's graveyard" is `changesZone(from = GRAVEYARD)` with any destination — hand,
 *    library, exile, the stack (cast from a graveyard) or the battlefield — keyed on
 *    `.ownedByOpponent()`: a card is only ever in its owner's graveyard (CR 400.3), so ownership,
 *    not control, names whose graveyard it left (same reasoning as Syr Konrad, the Grim).
 *  - "You may discard a card. If you do, return …" is `May(IfYouDo(discard, return))`; the return
 *    is the graveyard-guarded self-return, so it does nothing if the Titan left the graveyard
 *    before the trigger resolved.
 */
val ErebossTitan = card("Erebos's Titan") {
    manaCost = "{1}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Giant"
    oracleText = "As long as your opponents control no creatures, this creature has indestructible. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)\n" +
        "Whenever a creature card leaves an opponent's graveyard, you may discard a card. If you do, " +
        "return this card from your graveyard to your hand."
    power = 5
    toughness = 5

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, Filters.Self),
            condition = Conditions.OpponentControls(GameObjectFilter.Creature, negate = true),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.ownedByOpponent()).changesZone(from = Zone.GRAVEYARD)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            Effects.IfYouDo(
                action = Patterns.Hand.discardCards(1),
                then = Effects.ReturnToHandFromGraveyard(EffectTarget.Self),
            )
        )
        description = "You may discard a card. If you do, return Erebos's Titan from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "94"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5972911-2d41-4a0c-8a8f-b3d35a6594d8.jpg?1783938343"
        ruling("2015-06-22", "Damage dealt to a creature with indestructible remains marked on that creature. If Erebos's Titan has lethal damage marked on it and it loses indestructible (perhaps because your opponent controlled no creatures and then gained control of one), Erebos's Titan will be destroyed.")
        ruling("2015-06-22", "The last ability triggers only if Erebos's Titan is in your graveyard.")
        ruling("2015-06-22", "In a multiplayer game, the last ability won't trigger when a player with a creature card in their graveyard leaves the game.")
    }
}
