package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ExtendedAbsence = card("Extended Absence") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature or planeswalker. Extended Absence deals 1 damage to each opponent and you gain 1 life."

    spell {
        val permanent = target("target", Targets.CreatureOrPlaneswalker)
        effect = Effects.Exile(permanent)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)))
            .then(Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Nathaniel Himawan"
        flavorText = "\"On the bright side, the corpus inversion spell performed as expected.\"\n—Zim, Theorix mage"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb4b6ed8-782e-4473-abc9-d50bf2275c6a.jpg?1789556713"
        inBooster = false
    }
}
