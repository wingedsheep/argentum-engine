package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Inside Information
 * {X}{B}{B}
 * Sorcery
 *
 * Exile the top X cards of target opponent's library. You may play those cards this turn. If you
 * cast a spell this way, pay life equal to its mana value rather than pay its mana cost.
 *
 * The alternative cost composes three independent permissions: play the exiled collection, waive
 * each spell's mana cost, and require its mana value as a life payment. Lands retain normal play
 * timing and consume a land play, but naturally receive neither of the cast-only cost components.
 */
val InsideInformation = card("Inside Information") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile the top X cards of target opponent's library. You may play those cards " +
        "this turn. If you cast a spell this way, pay life equal to its mana value rather than " +
        "pay its mana cost."

    spell {
        target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.XValue, Player.TargetOpponent),
                storeAs = "insideInformationExiled",
            ),
            MoveCollectionEffect(
                from = "insideInformationExiled",
                destination = CardDestination.ToZone(Zone.EXILE, Player.TargetOpponent),
            ),
            Effects.GrantMayPlayFromExile("insideInformationExiled"),
            Effects.GrantPlayWithoutPayingCost("insideInformationExiled"),
            Effects.GrantPlayWithAdditionalCost(
                from = "insideInformationExiled",
                additionalCost = Costs.additional.PayLifeEqualToManaValueOfSpell,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "76"
        artist = "Sean Vo"
        flavorText = "\"Old fool!\" Bilbo thought. \"Why, there is a large patch in the hollow " +
            "of his left breast as bare as a snail out of its shell!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9763bd56-fa4b-4907-ad15-c3f040c5fc0a.jpg?1785152426"

        ruling(
            "2026-06-29",
            "If you cast a spell for another cost rather than pay its mana cost, you can't choose " +
                "another alternative cost. You may still pay optional additional costs and must " +
                "pay mandatory additional costs.",
        )
        ruling(
            "2026-06-29",
            "You must follow all normal timing rules when playing a land or casting a spell " +
                "exiled with Inside Information.",
        )
    }
}
