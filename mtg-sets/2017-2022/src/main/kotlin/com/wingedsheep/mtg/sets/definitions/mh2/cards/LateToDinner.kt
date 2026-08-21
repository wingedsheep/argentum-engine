package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Late to Dinner — Modern Horizons 2 #19
 * {3}{W} · Sorcery
 *
 * Return target creature card from your graveyard to the battlefield. Create a Food token. (It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 *
 * A white Zombify with a snack. `Targets.CreatureCardInYourGraveyard` is a graveyard-zoned target
 * requirement, so the legality check reads the graveyard rather than the battlefield;
 * [Effects.PutOntoBattlefieldFromGraveyard] carries the explicit `fromZone` so the move is a
 * graveyard-to-battlefield reanimation rather than a generic blink.
 *
 * The Food is a separate sentence, not a rider, so it is still created if the creature card has
 * left the graveyard by resolution.
 */
val LateToDinner = card("Late to Dinner") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield. Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        val creature = target("target creature card from your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature) then Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Kev Walker"
        flavorText = "\"I knew you were set in your ways, friend, but even I didn't expect you to keep our engagement, under the circumstances.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6633cab9-23f9-474e-96f1-ca7c0c67691c.jpg?1783926889"
    }
}
