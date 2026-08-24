package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Return to Battle
 * {B}
 * Sorcery
 * Return target creature card from your graveyard to your hand.
 */
val ReturnToBattle = card("Return to Battle") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to your hand."

    spell {
        val t = target("target", Targets.CreatureCardInYourGraveyard)
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Ding Songjian"
        flavorText = "\"Swallowing his eye, the valiant Xiahou Dun fought on; / But Cao Cao's vanguard, its commander wounded, could not hold out for long.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1841e615-fdcd-4187-bd69-d07abde0e1ae.jpg"
    }
}
