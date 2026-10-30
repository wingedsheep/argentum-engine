package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps

/**
 * Drumbellower — Neon Dynasty Commander #6 (canonical printing)
 * {2}{W} · Creature — Spirit · 2/1
 *
 * Flying
 * Untap all creatures you control during each other player's untap step.
 *
 * The creature-scoped Seedborn Muse: [UntapFilteredDuringOtherUntapSteps] over creatures, matched
 * against projected state so a crewed Vehicle or animated land counts while it's a creature.
 */
val Drumbellower = card("Drumbellower") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Untap all creatures you control during each other player's untap step."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = UntapFilteredDuringOtherUntapSteps(filter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Lie Setiawan"
        flavorText = "By the second night after the kami had drifted into town, the local general store " +
            "was completely sold out of earplugs."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/718c7383-d2e0-449b-b5b5-378d7c6e1fcb.jpg?1783923998"
        ruling(
            "2022-02-18",
            "Other effects can prevent a creature from untapping during an untap step. You do need to " +
                "look carefully, however, as many effects say that the creature doesn't untap during its " +
                "controller's untap step, and this card's ability applies during other players' untap " +
                "steps. If a card does say this, then Drumbellower can untap it."
        )
    }
}
