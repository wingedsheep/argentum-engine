package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Makindi Stampede {3}{W}{W} // Makindi Mesas — Zendikar Rising #26
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Makindi Stampede: "Creatures you control get +2/+2 until end of turn."
 * Back — Makindi Mesas: "This land enters tapped. {T}: Add {W}."
 *
 * The affected set of creatures is locked in on resolution; creatures arriving later are not pumped.
 */
private val MakindiStampedeFront = card("Makindi Stampede") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +2/+2 until end of turn."

    spell {
        effect = Patterns.Group.modifyStatsForAll(2, 2, Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Tomasz Jedruszek"
        flavorText = "\"They say the wind carved out these canyon walls. But the floor was hammered flat by hooves.\"\n—Kreq of Sunder Bay"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ada9a974-8f1f-4148-bd61-200fc14714b2.jpg?1783929418"
    }
}

private val MakindiMesasBack = card("Makindi Mesas") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "This land enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Tomasz Jedruszek"
        flavorText = "\"The ground is rumbling. Better hope it's just the Roil!\"\n—Kreq of Sunder Bay"
        imageUri = "https://cards.scryfall.io/normal/back/a/d/ada9a974-8f1f-4148-bd61-200fc14714b2.jpg?1783929418"
    }
}

val MakindiStampede: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = MakindiStampedeFront,
    backFace = MakindiMesasBack,
)
