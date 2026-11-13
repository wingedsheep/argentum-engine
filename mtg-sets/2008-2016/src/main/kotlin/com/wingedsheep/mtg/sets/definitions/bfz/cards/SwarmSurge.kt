package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swarm Surge
 * {2}{B}
 * Sorcery
 * Devoid (This card has no color.)
 * Creatures you control get +2/+0 until end of turn. Colorless creatures you control also gain
 * first strike until end of turn.
 *
 * Two group loops over the creatures you control as the spell resolves. The second one reads each
 * creature's projected colors, so devoid creatures and colorless artifact creatures qualify. Each
 * grant is fixed to the creature it landed on, so a creature that stops being colorless later in
 * the turn keeps first strike (per the ruling).
 */
val SwarmSurge = card("Swarm Surge") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Creatures you control get +2/+0 until end of turn. Colorless creatures you control also gain first strike until end of turn."

    keywords(Keyword.DEVOID)

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(2, 0, EffectTarget.IterationEntity)
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).youControl()),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Svetlin Velinov"
        flavorText = "The Eldrazi rampaged across Zendikar in a tide of chittering, heaving flesh."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/837287cc-f3a4-4ba7-8518-9b1e4a53acda.jpg?1783938204"
        ruling("2015-08-25", "A colorless creature given first strike by Swarm Surge won't lose first strike if it stops being colorless later in the turn.")
    }
}
