package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Domri Rade — Gatecrash #156 (canonical printing)
 * {1}{R}{G} · Legendary Planeswalker — Domri · Loyalty 3
 *
 * - +1: Frost Augur's look-and-take shape with a creature filter — a creature may be revealed into
 *   hand; anything not taken stays on top unrevealed (ruling).
 * - −2: Pit Fight's two-target fight, the second wrapped in [TargetOther] so it may be another
 *   creature you control but not the first target.
 * - −7: Vivien Reid's permanent keyword-granting emblem over creatures you control.
 */
val DomriRade = card("Domri Rade") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Domri"
    startingLoyalty = 3
    oracleText = "+1: Look at the top card of your library. If it's a creature card, you may reveal it and " +
        "put it into your hand.\n" +
        "−2: Target creature you control fights another target creature.\n" +
        "−7: You get an emblem with \"Creatures you control have double strike, trample, hexproof, and haste.\""

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val matching = filter(looked, GameObjectFilter.Creature)
            val toHand = chooseUpTo(
                1,
                from = matching,
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave on top of your library"
            )
            move(toHand, CardDestination.ToZone(Zone.HAND), revealed = true, revealToSelf = false)
        }
    }

    loyaltyAbility(-2) {
        val mine = target(TargetFilter.CreatureYouControl)
        val other = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.Fight(mine, other)
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter.AllCreaturesYouControl,
            grantedKeywords = listOf(
                Keyword.DOUBLE_STRIKE.name,
                Keyword.TRAMPLE.name,
                Keyword.HEXPROOF.name,
                Keyword.HASTE.name
            ),
            emblemDescription = "Creatures you control have double strike, trample, hexproof, and haste."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "156"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21b48170-99dd-440f-9954-fc229d6094d3.jpg?1783940109"
        ruling("2017-03-14", "When resolving Domri's first ability, if the card you look at isn't a creature card, or if it's a creature card you don't want to put into your hand, you simply put it back on top of your library. You don't reveal it or say why you're putting it back.")
        ruling("2017-03-14", "The second target of Domri's second ability can be another creature you control, but it can't be the same creature as the first target.")
        ruling("2017-03-14", "If either target of Domri's second ability is an illegal target when the ability tries to resolve, neither creature will deal or be dealt damage.")
        ruling("2024-01-12", "If a creature has both double strike and trample, the combat damage it assigned during the first combat damage step will be considered when determining how much damage can trample through in the second combat damage step. If each creature blocking it is destroyed during the first combat damage step, then all of the damage must be assigned to the player, planeswalker, or battle it's attacking during the second combat damage step.")
    }
}
