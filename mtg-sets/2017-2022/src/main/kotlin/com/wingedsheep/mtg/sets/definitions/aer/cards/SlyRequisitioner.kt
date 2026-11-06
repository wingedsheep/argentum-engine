package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sly Requisitioner
 * {4}{B}
 * Creature — Human Artificer
 * 2/2
 * Improvise
 * Whenever a nontoken artifact you control is put into a graveyard from the battlefield, create a
 * 1/1 colorless Servo artifact creature token.
 */
val SlyRequisitioner = card("Sly Requisitioner") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "Whenever a nontoken artifact you control is put into a graveyard from the battlefield, create a 1/1 " +
        "colorless Servo artifact creature token."

    keywords(Keyword.IMPROVISE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.nontoken().youControl()).dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2d05324-6d85-40da-a087-b5822bc8f42e.jpg"
        ruling("2017-02-09", "If Sly Requisitioner is put into a graveyard at the same time as a nontoken artifact you control, its ability triggers.")
        ruling("2017-02-09", "Improvise doesn't change a spell's mana cost or mana value.")
        ruling("2017-02-09", "When calculating a spell's total cost, include any alternative costs, additional costs, or anything else that increases or reduces the cost to cast the spell. Improvise applies after the total cost is calculated.")
        ruling("2017-02-09", "Because improvise isn't an alternative cost, it can be used in conjunction with alternative costs.")
        ruling("2017-02-09", "Improvise can't pay for {W}, {U}, {B}, {R}, {G}, or {C} mana symbols in a spell's total cost.")
        ruling("2017-02-09", "Improvise can't be used to pay for anything other than the cost of casting the spell. For example, it can't be used during the resolution of an ability that says \"Counter target spell unless its controller pays {3}.\"")
        ruling("2017-02-09", "If an artifact you control has a mana ability with {T} in the cost, activating that ability while casting a spell with improvise will result in the artifact being tapped when you pay the spell's costs. You won't be able to tap it again for improvise. Similarly, if you sacrifice an artifact to activate a mana ability while casting a spell with improvise, that artifact won't be on the battlefield when you pay the spell's costs, so you won't be able to tap it for improvise.")
        ruling("2017-02-09", "Tapping an artifact won't cause its abilities to stop applying unless those abilities say so.")
        ruling("2017-02-09", "Equipment attached to a creature doesn't become tapped when that creature becomes tapped, and tapping that Equipment doesn't cause the creature to become tapped.")
    }
}
