package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Priest of Fell Rites — Modern Horizons 2 #208
 * {W}{B} · Creature — Human Warlock · 2/2
 *
 * {T}, Pay 3 life, Sacrifice this creature: Return target creature card from your graveyard to the
 * battlefield. Activate only as a sorcery.
 * Unearth {3}{W}{B}
 *
 * The target is chosen before the costs are paid, so the Priest can't target itself (ruling).
 */
val PriestOfFellRites = card("Priest of Fell Rites") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Warlock"
    power = 2
    toughness = 2
    oracleText = "{T}, Pay 3 life, Sacrifice this creature: Return target creature card from your graveyard " +
        "to the battlefield. Activate only as a sorcery.\n" +
        "Unearth {3}{W}{B} ({3}{W}{B}: Return this card from your graveyard to the battlefield. It gains " +
        "haste. Exile it at the beginning of the next end step or if it would leave the battlefield. " +
        "Unearth only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(3), Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        val creatureCard = target(TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefield(creatureCard)
        description = "{T}, Pay 3 life, Sacrifice this creature: Return target creature card from your " +
            "graveyard to the battlefield. Activate only as a sorcery."
    }

    unearth("{3}{W}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Pauline Voss"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d116329d-6de7-402c-821d-e9aeba9ad2b1.jpg?1783926812"
        ruling(
            "2021-06-18",
            "You choose the target of the first ability before paying any of that ability's costs. In other " +
                "words, you can't choose Priest of Fell Rites itself as the target to bring it back from the graveyard."
        )
        ruling(
            "2021-06-18",
            "If a creature returned to the battlefield by the unearth ability would leave it for any reason, it's " +
                "exiled instead—unless the spell or ability that's causing the creature to leave the battlefield is " +
                "actually trying to exile it."
        )
    }
}
