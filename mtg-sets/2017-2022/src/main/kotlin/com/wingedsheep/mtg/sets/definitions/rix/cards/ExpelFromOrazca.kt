package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Expel from Orazca
 * {1}{U}
 * Instant
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Return target nonland permanent to its owner's hand. If you have the city's blessing, you may put
 * that permanent on top of its owner's library instead.
 *
 * Spell ascend is the first step of resolution (the Golden Demise shape), so a blessing granted by
 * this very spell already counts. With the blessing, the caster chooses top of library or hand
 * (the Whirlpool Whelm shape); declining returns it to hand, per the ruling.
 */
val ExpelFromOrazca = card("Expel from Orazca") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Return target nonland permanent to its owner's hand. If you have the city's blessing, you may put " +
        "that permanent on top of its owner's library instead."

    keywords(Keyword.ASCEND)

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        val bounce = Effects.ReturnToHand(permanent)
        effect = Effects.If(
            condition = Conditions.ControlPermanentsAtLeast(10),
            then = Effects.GainCitysBlessing()
        ) then Effects.If(
            condition = Conditions.YouHaveCitysBlessing,
            then = Effects.May(
                effect = Effects.PutOnTopOfLibrary(permanent),
                otherwise = bounce,
                prompt = "Put that permanent on top of its owner's library?"
            ),
            otherwise = bounce
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64f5119f-3921-48a5-a8fb-0cb1bbabde30.jpg"
        ruling(
            "2018-01-19",
            "If you have the city's blessing and choose not to put the target nonland permanent on top of its " +
                "owner's library, it's returned to its owner's hand."
        )
        ruling(
            "2018-01-19",
            "If you cast a spell with ascend, you don't get the city's blessing until it resolves. Players may " +
                "respond to that spell by trying to change whether you get the city's blessing."
        )
    }
}
