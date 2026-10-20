package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elder Land Wurm
 * {4}{W}{W}{W}
 * Creature — Dragon Wurm
 * 5/5
 * Defender, trample
 * When this creature blocks, it loses defender.
 *
 * The loss is permanent; if another effect later grants defender again, blocking
 * again removes that instance too.
 */
val ElderLandWurm = card("Elder Land Wurm") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dragon Wurm"
    power = 5
    toughness = 5
    oracleText = "Defender, trample\nWhen this creature blocks, it loses defender."

    keywords(Keyword.DEFENDER, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.RemoveKeyword(Keyword.DEFENDER, EffectTarget.Self, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Quinton Hoover"
        flavorText = "Sometimes it's best to let sleeping dragons lie."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef3651d4-969c-464d-a444-40a640d0c6ba.jpg?1783948087"
        ruling("2004-10-04", "Another effect can give Elder Land Wurm defender after it has lost it. Blocking again will cause it to lose that instance of defender too.")
    }
}
