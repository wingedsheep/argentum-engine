package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Mountain-king's Return
 * {2}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Recruit.
 * II — Return target creature card with mana value 3 or less from your graveyard to the battlefield.
 * III — Put a +1/+1 counter on up to one target creature.
 *
 * Chapter I is the bare [Patterns.Mechanic.recruit] facade — no target, so the chapter ability goes
 * on the stack untargeted and pauses for the discard choice when it resolves.
 *
 * Chapter II is the Helping Hand reanimation shape ([CardPredicate.IsCreature] +
 * [CardPredicate.ManaValueAtMost] `(3)` owned by you, in [Zone.GRAVEYARD]) but *untapped* — this
 * card omits Helping Hand's "tapped" rider.
 *
 * Chapter III's "up to one target" is `optional = true` on [TargetCreature]; declining leaves the
 * [Effects.AddCounters] a no-op, and the chapter still resolves and the Saga is still sacrificed.
 */
val TheMountainKingsReturn = card("The Mountain-king's Return") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Recruit. (Draw a card, then discard a card. If you discarded a nonland card, create a " +
        "1/1 white Human Soldier creature token.)\n" +
        "II — Return target creature card with mana value 3 or less from your graveyard to the battlefield.\n" +
        "III — Put a +1/+1 counter on up to one target creature."

    // I — Recruit.
    sagaChapter(1) {
        effect = Patterns.Mechanic.recruit()
    }

    // II — Return target creature card with mana value 3 or less from your graveyard to the battlefield.
    sagaChapter(2) {
        val reanimated = target(
            "target creature card with mana value 3 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter(
                        cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.ManaValueAtMost(3)),
                        controllerPredicate = ControllerPredicate.OwnedByYou,
                    ),
                    zone = Zone.GRAVEYARD,
                )
            )
        )
        effect = Effects.PutOntoBattlefield(reanimated)
    }

    // III — Put a +1/+1 counter on up to one target creature.
    sagaChapter(3) {
        val boosted = target(
            "up to one target creature",
            TargetCreature(count = 1, optional = true)
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, boosted)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68f4893d-e9a5-4f89-ade3-9ab78a834ad5.jpg?1784631780"
    }
}
