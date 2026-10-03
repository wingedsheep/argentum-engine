package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Sink into Stupor {1}{U}{U} // Soporific Springs
 * Instant
 * Return target spell or nonland permanent an opponent controls to its owner's hand.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {U}.
 *
 * "an opponent controls" restricts both halves of the target, so the controller predicate is
 * passed to the spell side as well as the permanent side. A returned spell is removed from the
 * stack — it isn't countered, so it works on uncounterable spells (2024-06-07 ruling).
 */
private val SinkIntoStuporFront = card("Sink into Stupor") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target spell or nonland permanent an opponent controls to its owner's hand."

    spell {
        val t = target(
            TargetSpellOrPermanent(
                permanentFilter = GameObjectFilter.NonlandPermanent.opponentControls(),
                spellFilter = GameObjectFilter.Any.opponentControls(),
                descriptionOverride = "target spell or nonland permanent an opponent controls",
            )
        )
        effect = Effects.ReturnSpellOrPermanentToOwnersHand(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Peter Polach"
        flavorText = "The kami help travelers find profound relief, by freeing them from their mortal concerns."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5358b87a-1a29-426d-b165-40c97da2c14d.jpg?1783911236"
        ruling("2024-06-07", "If a spell is returned to its owner's hand, it's removed from the stack and thus will not resolve. The spell isn't countered; it just no longer exists. This works even against a spell that can't be countered.")
        ruling("2024-06-07", "If a copy of a spell is returned to its owner's hand, it's moved there, then it will cease to exist as a state-based action. It can't be recast.")
    }
}

private val SoporificSpringsBack = card("Soporific Springs") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Peter Polach"
        flavorText = "Desperate travelers journey from across Kamigawa, drawn by rumors of relief from all their troubles."
        imageUri = "https://cards.scryfall.io/normal/back/5/3/5358b87a-1a29-426d-b165-40c97da2c14d.jpg?1783911236"
    }
}

val SinkIntoStupor: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SinkIntoStuporFront,
    backFace = SoporificSpringsBack,
)
