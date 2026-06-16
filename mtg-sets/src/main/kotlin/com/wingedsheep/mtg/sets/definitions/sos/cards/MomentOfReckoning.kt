package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Moment of Reckoning
 * {3}{W}{W}{B}{B}
 * Sorcery
 * Choose up to four. You may choose the same mode more than once.
 * • Destroy target nonland permanent.
 * • Return target nonland permanent card from your graveyard to the battlefield.
 *
 * "Choose up to four" with mode repetition = a modal spell with `chooseCount = 4`,
 * `minChooseCount = 0` (a player may always decline / pick fewer), and `allowRepeat = true`
 * (CR 700.2d — the same mode may be chosen more than once). Each chosen mode instance announces its
 * own target at cast time, so picking "Destroy" four times targets four distinct nonland permanents.
 * The reanimation mode uses [Effects.PutOntoBattlefield] on a nonland permanent card in the caster's
 * graveyard (same reanimate shape as Perennation).
 */
val MomentOfReckoning = card("Moment of Reckoning") {
    manaCost = "{3}{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Choose up to four. You may choose the same mode more than once.\n" +
        "• Destroy target nonland permanent.\n" +
        "• Return target nonland permanent card from your graveyard to the battlefield."

    spell {
        modal(chooseCount = 4, minChooseCount = 0, allowRepeat = true) {
            mode("Destroy target nonland permanent") {
                val t = target("target", TargetPermanent(filter = TargetFilter.NonlandPermanent))
                effect = Effects.Destroy(t)
            }
            mode("Return target nonland permanent card from your graveyard to the battlefield") {
                val t = target(
                    "target",
                    TargetObject(
                        filter = TargetFilter(
                            GameObjectFilter.NonlandPermanent.ownedByYou(),
                            zone = Zone.GRAVEYARD,
                        ),
                    ),
                )
                effect = Effects.PutOntoBattlefield(t)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Néstor Ossandón Leal"
        flavorText = "The archaics experience all of Arcavian history. Changing that fate " +
            "threatens their very existence."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/577d9dc8-7720-4dc9-b650-64b4729b309b.jpg?1775938423"
    }
}
