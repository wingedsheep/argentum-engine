package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nettle Drone — Battle for Zendikar #131
 * {2}{R} · Creature — Eldrazi Drone · 3/1
 * Devoid (This card has no color.)
 * {T}: This creature deals 1 damage to each opponent.
 * Whenever you cast a colorless spell, untap this creature.
 *
 * The cast trigger filters the spell on `CardPredicate.IsColorless` (Kozilek's Sentinel's shape), so a
 * devoid spell or a colorless artifact counts.
 */
val NettleDrone = card("Nettle Drone") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "{T}: This creature deals 1 damage to each opponent.\n" +
        "Whenever you cast a colorless spell, untap this creature."

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{T}: This creature deals 1 damage to each opponent."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless))
        effect = Effects.Untap(EffectTarget.Self)
        description = "Whenever you cast a colorless spell, untap this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e633833a-cc4b-4c8c-a0d3-cd263e09df81.jpg?1783938197"
    }
}
