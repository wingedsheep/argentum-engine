package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter

/**
 * Zegana, Utopian Speaker — Ravnica Allegiance #214
 * {2}{G}{U} · Legendary Creature — Merfolk Wizard · 4 / 4
 *
 * An intervening-if enters trigger (`Conditions.YouControl(... withCounter, excludeSelf = true)`),
 * an `Effects.Adapt(4)` activated ability, and the controller-only `GrantKeywordByCounter` trample
 * static shared with Trollbred Guardian.
 */
val ZeganaUtopianSpeaker = card("Zegana, Utopian Speaker") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 4
    toughness = 4
    oracleText = "When Zegana enters, if you control another creature with a +1/+1 counter on it, draw a card.\n" +
        "{4}{G}{U}: Adapt 4. (If this creature has no +1/+1 counters on it, put four +1/+1 counters on it.)\n" +
        "Each creature you control with a +1/+1 counter on it has trample."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE),
            excludeSelf = true
        )
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{G}{U}")
        effect = Effects.Adapt(4)
        description = "{4}{G}{U}: Adapt 4."
    }

    staticAbility {
        ability = GrantKeywordByCounter(Keyword.TRAMPLE, CounterType.PLUS_ONE_PLUS_ONE, controllerOnly = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd199a48-5ac8-4ab9-a33c-bbce6f7c9d1b.jpg?1783933632"
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
