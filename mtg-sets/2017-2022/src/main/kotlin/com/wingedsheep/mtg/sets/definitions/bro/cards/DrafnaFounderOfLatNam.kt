package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Drafna, Founder of Lat-Nam
 * {1}{U}
 * Legendary Creature — Human Artificer Advisor
 * 2/1
 * {1}{U}: Return target artifact you control to its owner's hand.
 * {3}, {T}: Copy target artifact spell you control. (The copy becomes a token.)
 *
 * The copy is [Effects.CopyTargetSpell] over a stack target filtered to artifact spells you
 * control; a copy of a permanent spell becomes a token as it resolves (CR 707.10f). The copy
 * clones the spell's current card characteristics, so a prototyped spell's copy keeps the
 * prototype's mana cost, colors and P/T (the 2022-10-14 ruling).
 */
val DrafnaFounderOfLatNam = card("Drafna, Founder of Lat-Nam") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Artificer Advisor"
    power = 2
    toughness = 1
    oracleText = "{1}{U}: Return target artifact you control to its owner's hand.\n" +
        "{3}, {T}: Copy target artifact spell you control. (The copy becomes a token.)"

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.youControl()))
        effect = Effects.ReturnToHand(artifact)
        description = "{1}{U}: Return target artifact you control to its owner's hand."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val spell = target(TargetFilter(GameObjectFilter.Artifact.youControl(), zone = Zone.STACK))
        effect = Effects.CopyTargetSpell(target = spell)
        description = "{3}, {T}: Copy target artifact spell you control. (The copy becomes a token.)"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Lie Setiawan"
        flavorText = "Alongside his wife, Hurkyl, Drafna built a legacy of scholarship that endured for millennia."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7355905-2b3b-4191-a89c-a0fc1494928b.jpg?1783920112"
        ruling(
            "2022-10-14",
            "A resolving copy of a permanent spell becomes a token, so the token isn't \"created.\" " +
                "Effects that care about a token being created won't interact with a token that enters " +
                "the battlefield because the triggered ability copied a permanent spell."
        )
        ruling(
            "2022-10-14",
            "If an effect copies a prototyped spell, that copy (as well as the token it becomes on the " +
                "battlefield) will have the same characteristics as the prototyped spell."
        )
    }
}
