package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sorin, Lord of Innistrad
 * {2}{W}{B}
 * Legendary Planeswalker — Sorin (loyalty 3)
 *
 * +1: Create a 1/1 black Vampire creature token with lifelink.
 * −2: You get an emblem with "Creatures you control get +1/+0."
 * −6: Destroy up to three target creatures and/or other planeswalkers. Return each card put
 *     into a graveyard this way to the battlefield under your control.
 *
 * The −6's targets are "a creature, or a planeswalker other than Sorin": the "other" binds to the
 * planeswalker branch only, so a Sorin that has become a creature can still target itself (ruling).
 *
 * The −6 pipeline destroys the targets tracking what actually moved (indestructible or
 * regenerated targets drop out), then keeps only the cards that are in a graveyard right now —
 * a target replaced into exile, or a commander sent to the command zone, was never "put into a
 * graveyard this way". Each survivor returns under your control; a token can't leave the
 * graveyard (CR 111.8), so a destroyed token stays gone (ruling).
 */
val SorinLordOfInnistrad = card("Sorin, Lord of Innistrad") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Sorin"
    startingLoyalty = 3
    oracleText = "+1: Create a 1/1 black Vampire creature token with lifelink.\n" +
        "−2: You get an emblem with \"$SORIN_LORD_OF_INNISTRAD_EMBLEM\"\n" +
        "−6: Destroy up to three target creatures and/or other planeswalkers. Return each card put " +
        "into a graveyard this way to the battlefield under your control."

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/0/d/0d6abdaa-7be5-48a1-acc9-b3cba4c10619.jpg?1783940786",
        )
    }

    loyaltyAbility(-2) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter.AllCreaturesYouControl,
            powerBonus = 1,
            emblemDescription = SORIN_LORD_OF_INNISTRAD_EMBLEM,
        )
    }

    loyaltyAbility(-6) {
        targets(
            TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Planeswalker.notSourceItself()),
            count = 3,
            optional = true,
        )
        effect = Effects.Pipeline {
            val targets = gather(CardSource.ChosenTargets)
            val destroyed = moveTracked(targets, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy)
            val inGraveyard = filter(destroyed, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD))
            move(inGraveyard, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "142"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27bb371f-d49f-41bd-bbe0-d5e1e2067e36.jpg?1783940803"

        ruling("2011-01-22", "The emblems are cumulative. If you get two of them, creatures you control will get +2/+0.")
        ruling("2011-01-22", "You can target a token creature with Sorin, Lord of Innistrad's third ability, but it won't return to the battlefield.")
        ruling("2011-01-22", "If a creature with undying is destroyed by Sorin, Lord of Innistrad's third ability, the undying ability will trigger, but the creature will already have been returned to the battlefield by Sorin. The undying ability won't do anything when it resolves.")
        ruling("2011-01-22", "If Sorin, Lord of Innistrad somehow becomes a creature, it can be chosen as a target of its own third ability. If Sorin, Lord of Innistrad is still on the battlefield when that ability resolves, it will then destroy Sorin and return him to the battlefield under your control.")
    }
}

private const val SORIN_LORD_OF_INNISTRAD_EMBLEM = "Creatures you control get +1/+0."
