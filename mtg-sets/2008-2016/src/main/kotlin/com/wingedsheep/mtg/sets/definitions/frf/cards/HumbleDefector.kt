package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Humble Defector — Fate Reforged #104 (canonical printing)
 * {1}{R} · Creature — Human Rogue · 2/1
 *
 * {T}: Draw two cards. Target opponent gains control of this creature. Activate only during your turn.
 */
val HumbleDefector = card("Humble Defector") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "{T}: Draw two cards. Target opponent gains control of this creature. " +
        "Activate only during your turn."

    activatedAbility {
        cost = Costs.Tap
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(2) then Effects.GiveControl(
            permanent = EffectTarget.Self,
            newController = opponent
        )
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        description = "{T}: Draw two cards. Target opponent gains control of this creature. " +
            "Activate only during your turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Slawomir Maniak"
        flavorText = "\"You were once Mardu, so your body and will are strong. Now we must train your mind.\"\n" +
            "—Houn, Jeskai elder"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bb0c13f-6cea-48b4-a5c8-2c0cfcfea7fd.jpg?1783938687"
        ruling(
            "2020-11-10",
            "If Humble Defector isn't on the battlefield as its ability resolves, but the target player is " +
                "still a legal target, the ability will resolve. You'll draw two cards, even though the " +
                "player doesn't gain control of Humble Defector."
        )
        ruling(
            "2020-11-10",
            "Humble Defector's ability can be activated any time during your turn, including in response " +
                "to a spell or ability."
        )
    }
}
