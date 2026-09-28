package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Lorwyn // Winnowing Forces — March of the Machine #236.
 * {4}{B}{G} · Battle — Siege · defense 5 // Creature — Elf Warrior, star/star
 *
 * When this Siege enters, destroy target non-Elf creature an opponent controls with power X or
 * less, where X is the number of lands you control.
 * // Winnowing Forces's power and toughness are each equal to the number of lands you control.
 *
 * The cap is `powerAtMostDynamic(landsYouControl())`, read when the target is chosen and again on
 * resolution (CR 608.2b), so losing a land in response can make the target illegal.
 */
private val InvasionOfLorwynFront = card("Invasion of Lorwyn") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, destroy target non-Elf creature an opponent controls with power X " +
        "or less, where X is the number of lands you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.notSubtype(Subtype.ELF).opponentControls()
                    .powerAtMostDynamic(DynamicAmounts.landsYouControl())
            )
        )
        effect = Effects.Destroy(creature)
        description = "When this Siege enters, destroy target non-Elf creature an opponent " +
            "controls with power X or less, where X is the number of lands you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93f623da-616e-4067-9ea8-5dfbeef8ce0b.jpg?1783916950"
    }
}

private val WinnowingForces = card("Winnowing Forces") {
    manaCost = ""
    colorIdentity = "BG"
    colorIndicator = "BG"
    typeLine = "Creature — Elf Warrior"
    oracleText = "Winnowing Forces's power and toughness are each equal to the number of lands you control."

    dynamicStats(DynamicAmounts.landsYouControl())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Dan Murayama Scott"
        flavorText = "The Phyrexian appearance was so offensive that perfects and eyeblights " +
            "worked together in unprecedented kinship to destroy them."
        imageUri = "https://cards.scryfall.io/normal/back/9/3/93f623da-616e-4067-9ea8-5dfbeef8ce0b.jpg?1783916950"
    }
}

val InvasionOfLorwyn: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfLorwynFront,
    backFace = WinnowingForces,
)
