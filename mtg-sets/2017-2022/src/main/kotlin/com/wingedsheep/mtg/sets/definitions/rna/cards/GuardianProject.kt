package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guardian Project
 * {3}{G}
 * Enchantment
 *
 * Whenever a nontoken creature you control enters, if it doesn't have the same name as another
 * creature you control or a creature card in your graveyard, draw a card.
 *
 * The intervening "if" (CR 603.4) is checked as the creature enters and again on resolution. It is
 * the negation of two existence checks over [GameObjectFilter.sharingNameWith] the triggering
 * creature:
 *  - "another creature you control" — [Conditions.YouControlAtLeastOtherThanTriggering], which
 *    drops the entering creature itself rather than Guardian Project;
 *  - "a creature card in your graveyard" — [Conditions.GraveyardContains]. If the entering creature
 *    dies with the ability on the stack, it is that creature card, so the recheck fails and no card
 *    is drawn (second ruling).
 * Names are compared exactly and a blank name never matches, so a face-down creature has no name to
 * share (fourth ruling).
 */
val GuardianProject = card("Guardian Project") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control enters, if it doesn't have the same name as another " +
        "creature you control or a creature card in your graveyard, draw a card."

    val sameName = GameObjectFilter.Creature.sharingNameWith(EffectTarget.TriggeringEntity)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).enters()
        interveningIf = Conditions.Not(
            Conditions.Any(
                Conditions.YouControlAtLeastOtherThanTriggering(1, sameName),
                Conditions.GraveyardContains(sameName),
            )
        )
        effect = Effects.DrawCards(1)
        description = "Whenever a nontoken creature you control enters, if it doesn't have the same name as " +
            "another creature you control or a creature card in your graveyard, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Chris Rallis"
        flavorText = "Simic's strength comes from its diversity."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccad6ce0-ddf0-458d-bdae-3d7805fdc775.jpg?1783933671"
        ruling(
            "2019-01-25",
            "Whether the entering creature shares a name with a creature you control or a creature card in your " +
                "graveyard is checked both as that creature enters and as Guardian Project's ability resolves. If " +
                "the entering creature isn't the first of its name as it enters, the ability doesn't trigger at " +
                "all; if its name is shared as the ability resolves, you don't draw a card."
        )
        ruling(
            "2019-01-25",
            "If the entering creature is put into your graveyard while Guardian Project's ability is on the " +
                "stack, that same card will be a creature card in your graveyard that shares a name with the " +
                "creature that was on the battlefield, so you won't draw a card."
        )
        ruling(
            "2019-01-25",
            "A face-down creature has no name, so it can't share a name with anything. This includes other " +
                "creatures with no name."
        )
    }
}
