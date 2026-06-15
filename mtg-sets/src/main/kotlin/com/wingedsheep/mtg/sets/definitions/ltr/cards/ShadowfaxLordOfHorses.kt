package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Shadowfax, Lord of Horses
 * {3}{R}{W}
 * Legendary Creature — Horse
 * 4/4
 *
 * Horses you control have haste.
 * Whenever Shadowfax attacks, you may put a creature card with lesser power from your
 * hand onto the battlefield tapped and attacking.
 *
 * Engine notes:
 * - "Horses you control have haste" is the standard keyword-anthem lord static:
 *   `GrantKeyword(HASTE, Creature.withSubtype(Horse).youControl())`. Shadowfax is itself
 *   a Horse you control, so it grants itself haste too — matching the literal wording.
 * - "with lesser power" = the chosen hand card's power is strictly less than Shadowfax's
 *   (the source's) power, modeled with `GameObjectFilter.Creature.powerLessThanEntity(Source)`.
 *   Hand cards are matched on their printed/characteristic power; Shadowfax's power is read
 *   from projected state (so anthems/+1/+1 counters on Shadowfax raise the cap).
 * - "tapped and attacking" reuses `Patterns.Hand.putFromHand(entersAttacking = true)`, which
 *   moves the chosen card to the battlefield with `ZonePlacement.TappedAndAttacking`; the
 *   `MoveCollectionExecutor`/`ZoneTransitionService` adds the `AttackingComponent` against the
 *   defending player. "you may" is the `ChooseUpTo(1)` selection (choosing zero declines).
 */
val ShadowfaxLordOfHorses = card("Shadowfax, Lord of Horses") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Horse"
    power = 4
    toughness = 4
    oracleText = "Horses you control have haste. (They can attack and {T} as soon as they come under your control.)\n" +
        "Whenever Shadowfax attacks, you may put a creature card with lesser power from your hand onto the battlefield tapped and attacking."

    // Horses you control have haste.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HORSE).youControl())
        )
    }

    // Whenever Shadowfax attacks, you may put a creature card with lesser power from your
    // hand onto the battlefield tapped and attacking.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature.powerLessThanEntity(EntityReference.Source),
            entersAttacking = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c7d861d-7832-4c15-8d6c-8c07a9a57891.jpg?1686970028"
    }
}
