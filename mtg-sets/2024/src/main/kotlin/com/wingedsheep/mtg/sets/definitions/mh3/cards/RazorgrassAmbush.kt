package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Razorgrass Ambush {1}{W} // Razorgrass Field
 * Instant
 * Razorgrass Ambush deals 3 damage to target attacking or blocking creature.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {W}.
 */
private val RazorgrassAmbushFront = card("Razorgrass Ambush") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Razorgrass Ambush deals 3 damage to target attacking or blocking creature."

    spell {
        val t = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Cristi Balanescu"
        flavorText = "When disturbed, razorgrass skewers unwary intruders in violent, jingling bursts."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57065dca-f90e-4184-bbc4-95d726a4160b.jpg?1783911235"
    }
}

private val RazorgrassFieldBack = card("Razorgrass Field") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Cristi Balanescu"
        flavorText = "Leonin hide their dwellings within the Razor Fields, making approach deadly without knowledge of their secret paths."
        imageUri = "https://cards.scryfall.io/normal/back/5/7/57065dca-f90e-4184-bbc4-95d726a4160b.jpg?1783911235"
    }
}

val RazorgrassAmbush: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = RazorgrassAmbushFront,
    backFace = RazorgrassFieldBack,
)
