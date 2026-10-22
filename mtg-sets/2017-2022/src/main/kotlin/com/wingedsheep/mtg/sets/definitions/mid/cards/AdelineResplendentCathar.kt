package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adeline, Resplendent Cathar (Innistrad: Midnight Hunt #1)
 * {1}{W}{W}
 * Legendary Creature — Human Knight
 * * /4
 * Vigilance
 * Adeline's power is equal to the number of creatures you control.
 * Whenever you attack, for each opponent, create a 1/1 white Human creature token that's tapped
 * and attacking that player or a planeswalker they control.
 *
 * The per-opponent loop rebinds `Controller` to the visited opponent, so that opponent is the
 * token's `attackedPlayer` while the token itself goes to Adeline's controller
 * (`Player.ControllerOfSource`). `CONTROLLER_CHOOSES` narrowed to that player lets you send each
 * token at the opponent or one of their planeswalkers — asked only when they control one. The
 * tokens weren't declared as attackers, so they don't trigger "whenever a creature attacks".
 */
val AdelineResplendentCathar = card("Adeline, Resplendent Cathar") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Vigilance\nAdeline's power is equal to the number of creatures you control.\n" +
        "Whenever you attack, for each opponent, create a 1/1 white Human creature token that's tapped " +
        "and attacking that player or a planeswalker they control."
    power = 0
    toughness = 4

    keywords(Keyword.VIGILANCE)
    dynamicPower(DynamicAmounts.creaturesYouControl())

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Human"),
                controller = EffectTarget.PlayerRef(Player.ControllerOfSource),
                tapped = true,
                attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                attackedPlayer = EffectTarget.Controller,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18092f68-b96e-4084-9eba-b240d2195d81.jpg?1783925668"
    }
}
