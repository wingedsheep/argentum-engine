package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Too Evil to Stay Dead — Marvel Super Heroes #118
 * {2}{B} · Sorcery
 *
 * Teamwork 4 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 4 or more.)
 * Choose target creature card in your graveyard with mana value 4 or less. If this spell was cast
 * using teamwork, instead choose target creature card in your graveyard. Return the chosen card to
 * the battlefield.
 *
 * The **teamwork-only targeting** shape of teamwork: "instead choose" replaces the target
 * requirement itself, not the effect's size, so the two branches are two different announcements.
 * CR 601.2c is the rules basis ("a spell may require alternative targets only if an alternative or
 * additional cost was chosen for it"); CR 702.194c supplies the other direction, that the plain
 * cast is announced as though the teamwork clause's target weren't there. That maps onto the
 * shared optional-additional-cost rail's `kickerTarget` / `kickerEffect` slots — the same ones
 * Fight with Fire and Brave the Wilds ride — serving teamwork here. A teamwork cast can therefore
 * reanimate a creature card the plain cast could not even target.
 *
 * The mana value is the graveyard card's own printed value; a card in a graveyard has no
 * continuous effects applied to it, so no projection is involved on either branch. Both branches
 * end in the same [Effects.PutOntoBattlefield] because "return the chosen card to the battlefield"
 * is the shared last sentence — the branch replaces the whole effect, so it has to restate it.
 */
val TooEvilToStayDead = card("Too Evil to Stay Dead") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Teamwork 4 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 4 or more.)\n" +
        "Choose target creature card in your graveyard with mana value 4 or less. If this spell " +
        "was cast using teamwork, instead choose target creature card in your graveyard. Return " +
        "the chosen card to the battlefield."

    teamwork(4)

    spell {
        val cheapCreature = target(
            "target creature card in your graveyard with mana value 4 or less",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard.manaValueAtMost(4)),
        )
        effect = Effects.PutOntoBattlefield(cheapCreature)

        val anyCreature = kickerTarget(
            "target creature card in your graveyard",
            Targets.CreatureCardInYourGraveyard,
        )
        kickerEffect = Effects.PutOntoBattlefield(anyCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Ioannis Fiore"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f471e9ce-73bb-4090-98f2-f591c7cf4efe.jpg?1783902936"
    }
}
