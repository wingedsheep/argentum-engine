package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * The Ur-Dragon — Commander 2017 #48 (earliest printing; reprinted in Commander Masters).
 *
 * {4}{W}{U}{B}{R}{G} Legendary Creature — Dragon Avatar, 10/10
 *   Eminence — As long as The Ur-Dragon is in the command zone or on the battlefield, other Dragon
 *   spells you cast cost {1} less to cast.
 *   Flying
 *   Whenever one or more Dragons you control attack, draw that many cards, then you may put a
 *   permanent card from your hand onto the battlefield.
 *
 * **Eminence** is an ability word with no rules meaning; the printed zone clause is what matters
 * (CR 113.6b: an ability that states which zones it functions in functions only from those zones).
 * The static twin of Edgar Markov's `triggerZones`: `activeZones = setOf(BATTLEFIELD, COMMAND)`
 * builds the bare battlefield static plus a `ZoneScopedStaticAbility` over the command zone, so the
 * discount is on in either zone and off in every other. In the command zone "you" is the owner.
 *
 * **"Other"** needs no filter: a spell is on the stack as its cost is determined (CR 601.2a,
 * 601.2f), where The Ur-Dragon's eminence doesn't function, so casting it from the command zone
 * isn't discounted — the cost calculator excludes the card being cast as a source.
 *
 * The attack trigger is `Triggers.you.attacks(with = Dragon)` — once per declaration however many
 * players are attacked (ruling), capturing the declared Dragons, so "that many" is
 * `distinctEntitiesIn(TriggerCaptured)` and still counts a Dragon that left before resolution
 * (ruling). A Dragon put onto the battlefield attacking was never declared and doesn't trigger it
 * (ruling). "You may put a permanent card" is `Patterns.Hand.putFromHand(Permanent)`, choosing up
 * to one.
 */
val TheUrDragon = card("The Ur-Dragon") {
    manaCost = "{4}{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Dragon Avatar"
    power = 10
    toughness = 10
    oracleText = "Eminence — As long as The Ur-Dragon is in the command zone or on the battlefield, " +
        "other Dragon spells you cast cost {1} less to cast.\n" +
        "Flying\n" +
        "Whenever one or more Dragons you control attack, draw that many cards, then you may put a " +
        "permanent card from your hand onto the battlefield."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype(Subtype.DRAGON)),
            modification = CostModification.ReduceGeneric(1),
        )
        activeZones = setOf(Zone.BATTLEFIELD, Zone.COMMAND)
    }

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.attacks(with = GameObjectFilter.Creature.withSubtype(Subtype.DRAGON))
        effect = Effects.DrawCards(DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured)) then
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Permanent)
        description = "Whenever one or more Dragons you control attack, draw that many cards, then " +
            "you may put a permanent card from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "48"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e78b70b-0c67-4f14-8ad7-c9f8e3f59743.jpg?1783935933"

        ruling("2017-08-25", "You draw one card for each Dragon you controlled that attacked, even if some of them left the battlefield before The Ur-Dragon's triggered ability resolves.")
        ruling("2017-08-25", "You may only put one permanent card from your hand onto the battlefield, no matter how many Dragons you attacked with or how many players they attacked.")
        ruling("2017-08-25", "If a Dragon you control enters the battlefield attacking, it won't cause The Ur-Dragon's last ability to trigger.")
        ruling("2017-08-25", "The Ur-Dragon's triggered ability resolves after all attackers have been chosen. You can't attack with some Dragons, put a creature card with haste onto the battlefield, and then attack with that creature. Similarly, any \"whenever a creature attacks\" abilities of the permanent card you put onto the battlefield won't trigger.")
    }
}
