package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

private val aura = GameObjectFilter.Enchantment.withSubtype("Aura")

/**
 * Pearl-Ear, Imperial Advisor
 * {1}{W}{W}
 * Legendary Creature — Fox Advisor
 * 3/4
 * Lifelink
 * Enchantment spells you cast have affinity for Auras. (They cost {1} less to cast for each Aura
 * you control.)
 * Whenever you cast an Aura spell that targets a modified permanent you control, draw a card.
 *
 * "Affinity for Auras" is the generic reduction `ReduceGenericBy(PermanentsYouControlMatching(Aura))`
 * applied to every enchantment spell you cast; the count is read for the caster, so an Aura you
 * control on an opponent's permanent still counts. "Modified" is [StatePredicate.IsModified]
 * (CR 700.9): a counter, an Equipment, or an Aura controlled by the permanent's controller.
 */
val PearlEarImperialAdvisor = card("Pearl-Ear, Imperial Advisor") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Fox Advisor"
    power = 3
    toughness = 4
    oracleText = "Lifelink\n" +
        "Enchantment spells you cast have affinity for Auras. (They cost {1} less to cast for each Aura you control.)\n" +
        "Whenever you cast an Aura spell that targets a modified permanent you control, draw a card. " +
        "(Equipment, Auras you control, and counters are modifications.)"

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Enchantment),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(aura)
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            spell = aura,
            requires = setOf(
                SpellCastPredicate.TargetsMatching(
                    GameObjectFilter.Permanent.youControl().withStatePredicate(StatePredicate.IsModified)
                )
            )
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28aef7c8-58b3-463c-91d3-2d1ff8a815ee.jpg?1783911298"
        ruling("2024-06-07", "If a spell has multiple instances of affinity, each one applies. For example, if you somehow control two Pearl-Ear, Imperial Advisors and you control two Auras, each enchantment spell you cast will cost {4} less to cast.")
        ruling("2024-06-07", "A creature you control that's the target of an Aura spell isn't modified unless it already has a counter on it, an Equipment attached to it, or an Aura you control attached to it.")
        ruling("2024-06-07", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
        ruling("2024-06-07", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2024-06-07", "An Aura controlled by another player does not cause a creature you control to be modified.")
        ruling("2024-06-07", "If you put an Aura on an opponent's permanent, you still control the Aura, and it still counts for your spells that have affinity for Auras.")
    }
}
