package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RepeatKeywordAction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tekuthal, Inquiry Dominus
 * {2}{U}{U}
 * Legendary Creature — Phyrexian Horror
 * 3/5
 *
 * Flying
 * If you would proliferate, proliferate twice instead.
 * {1}{U/P}{U/P}, Remove three counters from among other artifacts, creatures, and planeswalkers
 * you control: Put an indestructible counter on Tekuthal.
 */
val TekuthalInquiryDominus = card("Tekuthal, Inquiry Dominus") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Phyrexian Horror"
    power = 3
    toughness = 5
    oracleText = "Flying\n" +
        "If you would proliferate, proliferate twice instead.\n" +
        "{1}{U/P}{U/P}, Remove three counters from among other artifacts, creatures, and " +
        "planeswalkers you control: Put an indestructible counter on Tekuthal. " +
        "({U/P} can be paid with either {U} or 2 life.)"

    keywords(Keyword.FLYING)

    replacementEffect(RepeatKeywordAction(times = 2, appliesTo = EventPattern.ProliferatedEvent()))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U/P}{U/P}"),
            Costs.RemoveCounters(
                count = 3,
                counterType = null,
                filter = (GameObjectFilter.Artifact or GameObjectFilter.CreatureOrPlaneswalker).notSourceItself()
            )
        )
        effect = Effects.AddCounters(CounterType.INDESTRUCTIBLE, 1, EffectTarget.Self)
        description = "{1}{U/P}{U/P}, Remove three counters from among other artifacts, creatures, " +
            "and planeswalkers you control: Put an indestructible counter on Tekuthal."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "71"
        artist = "Martin de Diego Sádaba"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1d4b157-2c77-4355-8c65-78dec9d44c85.jpg?1783918056"
    }
}
