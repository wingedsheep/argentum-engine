package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Saheeli's Artistry
 * {4}{U}{U}
 * Sorcery
 *
 * Choose one or both —
 * • Create a token that's a copy of target artifact.
 * • Create a token that's a copy of target creature, except it's an artifact in addition to its
 *   other types.
 *
 * "Choose one or both" is the modal count (`chooseCount = 2`, `minChooseCount = 1`), as on Subtle
 * Strike. Each mode carries its own target, so choosing both may target one artifact creature twice.
 * The second mode's "artifact in addition to its other types" is `addCardTypes`, which folds into the
 * copy's [com.wingedsheep.sdk.scripting.effects.CopyExceptions] — a copiable value, per the ruling —
 * the same shape Molten Duplication and Saheeli, the Sun's Brilliance use.
 */
val SaheelisArtistry = card("Saheeli's Artistry") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Create a token that's a copy of target artifact.\n" +
        "• Create a token that's a copy of target creature, except it's an artifact in addition to its other types."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Create a token that's a copy of target artifact") {
                val t = target(TargetFilter.Artifact)
                effect = Effects.CreateTokenCopyOfTarget(target = t)
            }
            mode("Create a token that's a copy of target creature, except it's an artifact") {
                val t = target(TargetFilter.Creature)
                effect = Effects.CreateTokenCopyOfTarget(
                    target = t,
                    addCardTypes = setOf("ARTIFACT"),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Kieran Yanner"
        flavorText = "\"From the first touch, I know exactly what the metal wants to be.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42237c1d-5579-4c15-b97b-bcaaaf0b1ab2.jpg?1783937215"
        ruling("2016-09-20", "If you chose the second mode, the token is an artifact in addition to its other types. This is a copiable value of the token that other effects may copy.")
        ruling("2016-09-20", "If you choose both modes, you may target a single artifact creature twice and get two copies of that artifact creature.")
        ruling("2016-09-20", "If you choose both modes, the tokens enter one at a time. Thus, if the token created by the second mode has an ability that triggers \"whenever an artifact enters the battlefield,\" the token created by the first mode will already be on the battlefield and won't cause that ability to trigger.")
    }
}
