package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akroan Horse — Theros #210
 * {4} · Artifact Creature — Horse · 0/4
 *
 * Defender
 * When this creature enters, an opponent gains control of it.
 * At the beginning of your upkeep, each opponent creates a 1/1 white Soldier creature token.
 *
 * The enters trigger isn't targeted (ruling): the controller picks the opponent as it resolves —
 * [Effects.ChooseOpponent] read back through [Player.ChosenOpponent] (the Rohgahh of Kher Keep
 * shape); the control change has no duration. "Your upkeep" and "each opponent" are relative to the
 * Horse's *current* controller, so once it changes hands it triggers on the new controller's upkeep
 * and the original caster gets the Soldiers.
 */
val AkroanHorse = card("Akroan Horse") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Horse"
    power = 0
    toughness = 4
    oracleText = "Defender\nWhen this creature enters, an opponent gains control of it.\n" +
        "At the beginning of your upkeep, each opponent creates a 1/1 white Soldier creature token."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ChooseOpponent("Choose an opponent to gain control of Akroan Horse") then
            Effects.GiveControl(
                permanent = EffectTarget.Self,
                newController = EffectTarget.PlayerRef(Player.ChosenOpponent),
            )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            controller = EffectTarget.PlayerRef(Player.EachOpponent),
            imageUri = "https://cards.scryfall.io/normal/front/3/1/31f320f4-baae-46cb-840e-421f010a0a20.jpg?1783939699",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec95a60f-5164-47f9-9430-11b114c87815.jpg?1783939722"
        ruling(
            "2013-09-15",
            "Akroan Horse's enters-the-battlefield ability doesn't target any opponent. In a multiplayer " +
                "game, you choose the opponent as the ability resolves."
        )
        ruling(
            "2013-09-15",
            "In the last ability, \"each opponent\" refers to opponents of Akroan Horse's controller. In most " +
                "situations in two-player games, your opponent will control Akroan Horse and you'll put " +
                "Soldier tokens onto the battlefield."
        )
    }
}
