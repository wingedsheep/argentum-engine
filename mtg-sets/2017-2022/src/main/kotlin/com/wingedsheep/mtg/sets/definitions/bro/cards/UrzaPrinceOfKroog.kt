package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Urza, Prince of Kroog — The Brothers' War #226.
 * {2}{W}{U} · Legendary Creature — Human Artificer · 2/3
 *
 * Artifact creatures you control get +2/+2.
 * {6}: Create a token that's a copy of target artifact you control, except it's a 1/1 Soldier
 * creature in addition to its other types.
 *
 * The anthem is the Tempered Steel lord. The copy clause is the Nexus of Becoming shape: a 1/1
 * base P/T override plus the Soldier subtype and CREATURE card type unioned onto the copied type
 * line — copy exceptions, so they are part of the token's copiable values (per the rulings).
 */
val UrzaPrinceOfKroog = card("Urza, Prince of Kroog") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 3
    oracleText = "Artifact creatures you control get +2/+2.\n" +
        "{6}: Create a token that's a copy of target artifact you control, except it's a 1/1 Soldier creature in addition to its other types."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(GameObjectFilter.ArtifactCreature.youControl()),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{6}")
        val artifact = target(TargetFilter.Artifact.youControl())
        effect = Effects.CreateTokenCopyOfTarget(
            target = artifact,
            overridePower = 1,
            overrideToughness = 1,
            addedSubtypes = setOf(Subtype("Soldier")),
            addCardTypes = setOf(CardType.CREATURE.name),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Joshua Raphael"
        flavorText = "The prince consort was happy to let Yotia's coffers fund his extravagant research."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a7329cd-95af-4d71-984f-f5f28982520c.jpg?1783920026"
        ruling("2022-10-14", "The token is a Soldier creature in addition to its other types. Its base power and toughness is 1/1. These are the copiable values of the token's characteristics that other effects may copy.")
        ruling("2022-10-14", "If the copied permanent is copying something else, then the token enters the battlefield as whatever that permanent copied, with the listed exceptions.")
    }
}
