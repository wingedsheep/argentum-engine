package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oketra's Last Mercy
 * {1}{W}{W}
 * Sorcery
 * Your life total becomes equal to your starting life total. Lands you control don't untap during
 * your next untap step.
 *
 * Setting the life total gains or loses the difference, so it can lower a life total
 * above the starting one. The untap clause covers every land you control at that untap step, not a
 * set locked at resolution (2017-07-14 ruling).
 */
val OketrasLastMercy = card("Oketra's Last Mercy") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Your life total becomes equal to your starting life total. Lands you control don't untap during your next untap step."

    spell {
        effect = Effects.SetLifeTotal(DynamicAmounts.startingLifeTotal()) then
            Effects.SkipUntap(EffectTarget.Controller, GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Howard Lyon"
        flavorText = "To the end, all Oketra did was for others."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4d3bf26-6003-4699-a26e-b3b5bcd3e8dc.jpg?1783936059"
        ruling("2017-07-14", "For your life total to become your starting life total (normally 20), you gain or lose the appropriate amount of life. For example, if your life total is 4 when Oketra's Last Mercy resolves, it will cause you to gain 16 life; alternatively, if your life total is 25 when it resolves, it will cause you to lose 5 life. Other cards that interact with life gain or life loss will interact with this effect accordingly.")
        ruling("2017-07-14", "No lands that you control will untap during your next untap step, even lands that aren't tapped as this spell resolves. This includes lands that enter the battlefield after this spell resolves.")
        ruling("2017-07-14", "If more than one spell says that lands you control don't untap during your next untap step, the effects will all wear off during that untap step. You'll untap lands you control during your untap step after that one.")
    }
}
