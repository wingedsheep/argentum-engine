package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Glimpse the Impossible
 * {2}{R}
 * Sorcery
 *
 * Exile the top three cards of your library. You may play those cards this turn. At the beginning
 * of the next end step, if any of those cards remain exiled, put them into your graveyard, then
 * create a 0/1 colorless Eldrazi Spawn creature token for each card put into your graveyard this
 * way. Those tokens have "Sacrifice this token: Add {C}."
 *
 * Modeling notes:
 *  - Impulse draw spelled out as gather → exile → grant so the exiled pile has a name the delayed
 *    trigger can carry (`carryCollections`, CR 603.7c "those cards").
 *  - A carried card that has since changed zones (played, or cast and countered back into exile —
 *    a new object either way, CR 400.7) is dropped when the trigger fires, so the carried pile is
 *    exactly "those cards [that] remain exiled".
 *  - The spawn count reads the *tracked* move, so a card a replacement keeps out of the graveyard
 *    doesn't mint a token ("for each card put into your graveyard this way"). With nothing left the
 *    move is empty and zero tokens are made, which is what the intervening "if" yields.
 */
val GlimpseTheImpossible = card("Glimpse the Impossible") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile the top three cards of your library. You may play those cards this turn. " +
        "At the beginning of the next end step, if any of those cards remain exiled, put them into " +
        "your graveyard, then create a 0/1 colorless Eldrazi Spawn creature token for each card put " +
        "into your graveyard this way. Those tokens have \"Sacrifice this token: Add {C}.\""

    spell {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(3))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(from = exiled, expiry = MayPlayExpiry.EndOfTurn))
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    val milled = moveTracked(exiled, CardDestination.ToZone(Zone.GRAVEYARD))
                    run(Effects.CreateEldraziSpawn(milled.count))
                },
                carryCollections = listOf(exiled.key)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Justine Jones"
        flavorText = "Gisela beheld true divinity, and was humbled."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/133ad0dd-5b61-4c38-9264-0b0e75b95d95.jpg?1783911271"
    }
}
