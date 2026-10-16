package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Riveteers Ascendancy
 * {B}{R}{G}
 * Enchantment
 *
 * Whenever you sacrifice a creature, you may return target creature card with lesser mana value
 * from your graveyard to the battlefield tapped. Do this only once each turn.
 *
 * Modeling notes:
 *  - `Triggers.you.sacrifices(Creature)` fires once per sacrificed creature (CR 603.2c), with the
 *    sacrificed permanent as the triggering entity.
 *  - "Lesser mana value" is a strict `<`: `manaValueAtMostDynamic(triggeringManaValue() - 1)`, the
 *    Jackdaw Savior shape. The triggering entity has left the battlefield, so the evaluator reads
 *    its last-known mana value — a sacrificed token (gone from the graveyard) still caps correctly.
 *  - "Do this only once each turn" is CR 603.2h, keyed to the return rather than the trigger:
 *    `effectOncePerTurn = true`, never `oncePerTurn`. Declining the "you may" spends nothing, so
 *    the next sacrifice triggers again; once a creature is returned the ability stops triggering
 *    for the turn and instances already on the stack do nothing (2022-04-29 rulings).
 */
val RiveteersAscendancy = card("Riveteers Ascendancy") {
    manaCost = "{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Enchantment"
    oracleText = "Whenever you sacrifice a creature, you may return target creature card with " +
        "lesser mana value from your graveyard to the battlefield tapped. Do this only once each turn."

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Creature)
        val creatureCard = target(
            TargetFilter.CreatureInYourGraveyard.manaValueAtMostDynamic(
                DynamicAmounts.triggeringManaValue() - 1
            ),
        )
        effectOncePerTurn = true
        effect = Effects.May(Effects.PutOntoBattlefieldFromGraveyard(creatureCard, tapped = true))
        description = "Whenever you sacrifice a creature, you may return target creature card with " +
            "lesser mana value from your graveyard to the battlefield tapped. Do this only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Svetlin Velinov"
        flavorText = "\"We are this city, down to its bones.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21235916-2379-470c-b9ee-84246043d703.jpg?1783923073"
        ruling("2022-04-29", "\"Do this only once each turn\" refers only to the ability it is attached to, not other instances of the same ability. In other words, if you control multiple copies of Riveteers Ascendancy, you will be able to do this once for each of them.")
        ruling("2022-04-29", "Once you have chosen to return a creature to the battlefield, further instances of sacrificing creatures the same turn will not cause the ability to trigger.")
        ruling("2022-04-29", "If multiple instances of the ability are on the stack, you will be able to return a creature to the battlefield for only one of those instances. Once you do that, other instances will do nothing as they resolve.")
    }
}
