package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * King's Assassin
 * {1}{B}{B}
 * Creature — Human Assassin
 * 1/1
 * {T}: Destroy target tapped creature. Activate only during your turn,
 * before attackers are declared.
 */
val KingsAssassin = card("King's Assassin") {
    manaCost = "{1}{B}{B}"
    typeLine = "Creature — Human Assassin"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        val t = target("target", TargetCreature(filter = TargetFilter.TappedCreature))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Ron Spencer"
        flavorText = "The king rules by day, but the assassin rules by night."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/670a3149-54fb-4fd2-bc66-3ee9bcc3519d.jpg"
    }
}
